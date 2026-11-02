package com.tagflow.app.nfc

import android.nfc.Tag
import android.nfc.tech.NfcA
import com.tagflow.app.core.toHex
import java.io.IOException

/** Low-level helpers for NXP NTAG213/215/216 (password protection and memory dump). */
object NtagOps {

    class Layout(val name: String, val cfg: Int, val userBytes: Int, val pages: Int)

    fun layoutOf(cc2: Int): Layout? = when (cc2) {
        0x12 -> Layout("NTAG213", 0x29, 144, 45)
        0x3E -> Layout("NTAG215", 0x83, 504, 135)
        0x6D -> Layout("NTAG216", 0xE3, 888, 231)
        else -> null
    }

    private val PACK = b(0x54, 0x46, 0x00, 0x00) // "TF"

    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    private fun NfcA.readPage(p: Int): ByteArray = transceive(b(0x30, p))

    private fun NfcA.writePage(p: Int, d: ByteArray) {
        transceive(b(0xA2, p, d[0].toInt(), d[1].toInt(), d[2].toInt(), d[3].toInt()))
    }

    private fun pwdBytes(s: String): ByteArray {
        require(s.length == 4 && s.all { it.code < 256 }) { "Password must be exactly 4 characters" }
        return s.toByteArray(Charsets.ISO_8859_1)
    }

    private inline fun <T> session(tag: Tag, block: (NfcA, Layout) -> T): T {
        val a = NfcA.get(tag) ?: error("This tag doesn't support password protection")
        a.connect()
        a.timeout = 3000
        try {
            val cc = a.readPage(3)
            val lay = layoutOf(cc[2].toInt() and 0xFF)
                ?: error("Password protection works on NTAG213/215/216 only")
            return block(a, lay)
        } finally {
            runCatching { a.close() }
        }
    }

    fun setPassword(tag: Tag, password: String, protectRead: Boolean) {
        val pwd = pwdBytes(password)
        session(tag) { a, lay ->
            val cfg = a.readPage(lay.cfg)
            val auth0 = cfg[3].toInt() and 0xFF
            if (auth0 <= lay.cfg + 3) error("This tag already has a password — remove it first")
            a.writePage(lay.cfg + 2, pwd)           // PWD
            a.writePage(lay.cfg + 3, PACK)          // PACK
            val access = cfg.copyOfRange(4, 8)
            access[0] = ((access[0].toInt() and 0x78) or (if (protectRead) 0x80 else 0x00)).toByte()
            a.writePage(lay.cfg + 1, access)        // ACCESS (unlimited auth attempts)
            val c0 = cfg.copyOfRange(0, 4)
            c0[3] = 0x04                            // AUTH0: protect from page 4 onwards
            a.writePage(lay.cfg, c0)
        }
    }

    fun removePassword(tag: Tag, password: String) {
        val pwd = pwdBytes(password)
        session(tag) { a, lay ->
            val resp = try {
                a.transceive(b(0x1B, pwd[0].toInt(), pwd[1].toInt(), pwd[2].toInt(), pwd[3].toInt()))
            } catch (e: IOException) {
                error("Wrong password")
            }
            if (resp.size < 2 || resp[0] != PACK[0] || resp[1] != PACK[1]) error("Wrong password")
            val cfg = a.readPage(lay.cfg)
            val c0 = cfg.copyOfRange(0, 4)
            c0[3] = 0xFF.toByte()
            a.writePage(lay.cfg, c0)                // AUTH0 = off
            val access = cfg.copyOfRange(4, 8)
            access[0] = (access[0].toInt() and 0x78).toByte()
            a.writePage(lay.cfg + 1, access)
            a.writePage(lay.cfg + 2, b(0xFF, 0xFF, 0xFF, 0xFF))
            a.writePage(lay.cfg + 3, b(0, 0, 0, 0))
        }
    }

    fun dump(tag: Tag): List<String> = session(tag) { a, lay ->
        val lines = mutableListOf<String>()
        var p = 0
        while (p < lay.pages) {
            val chunk = try {
                a.readPage(p)
            } catch (e: IOException) {
                lines += "—  protected from page $p  —"
                break
            }
            for (i in 0 until 4) {
                val page = p + i
                if (page >= lay.pages) break
                val d = chunk.copyOfRange(i * 4, i * 4 + 4)
                val ascii = d.map { c ->
                    val ch = c.toInt() and 0xFF
                    if (ch in 32..126) ch.toChar() else '.'
                }.joinToString("")
                lines += "%03d  %s  %s".format(page, d.toHex(" "), ascii)
            }
            p += 4
        }
        lines
    }
}

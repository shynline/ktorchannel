package com.github.shynline

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import kotlin.collections.HashMap

internal fun String.deserializeToHashMap(): HashMap<String, String>{
    val data: ByteArray = Base64.getDecoder().decode(this)
    val ois = ObjectInputStream(ByteArrayInputStream(data))
    val o = ois.readObject()
    ois.close()
    @Suppress("UNCHECKED_CAST")
    return o as HashMap<String, String>
}
internal fun HashMap<String, String>.serialize() : String{
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(this)
    oos.close()
    return Base64.getEncoder().encodeToString(baos.toByteArray())
}
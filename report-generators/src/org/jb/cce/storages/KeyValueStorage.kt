package org.jb.cce.storages

interface KeyValueStorage<T, V> {
    fun get(key: T): V
    fun getKeys(): List<T>
    fun save(baseKey: T, value: V): T
}
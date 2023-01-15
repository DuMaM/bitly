package pl.nowak.bitly

import java.util.concurrent.atomic.AtomicInteger

class CustomCircularArray<T> : Iterable<T>, Cloneable {

    /**
     * Creates a new instance of the array with the given size.
     */
    constructor(bufferSize: Int) {
        this.arr = arrayOfNulls(bufferSize)
    }

    /**
     * Creates a new instance of the array as a copy.
     */
    constructor(customCircularArray: CustomCircularArray<T>) {
        this.arr = customCircularArray.arr.copyOf()
        this._size = customCircularArray._size
        this._tail = customCircularArray._tail
    }

    private val arr: Array<Any?>
    private var _size: Int = 0
    private var _tail: Int = -1

    @Suppress("unused")
    val tail: Int
        get() = _tail

    @Suppress("unused")
    val head: Int
        get() = if (_size == arr.size) (_tail + 1) % _size else 0

    /**
     * Number of elements currently stored in the array.
     */
    val size: Int
        get() = _size

    fun clean() {
        _size = 0
        _tail = -1
    }

    /**
     * Add an element to the array.
     */
    fun add(item: T) {
        _tail = (_tail + 1) % arr.size
        arr[_tail] = item
        if (_size < arr.size) _size++
    }

    /**
     * Get an element from the array.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(index: Int): T =
        when {
            _size == 0 || index > _size || index < 0 -> throw IndexOutOfBoundsException("$index")
            _size == arr.size -> arr[(head + index) % arr.size]
            else -> arr[index]
        } as T

    /**
     * This array as a list.
     */
    @Suppress("unused")
    fun toList(): List<T> = iterator().asSequence().toList()

    public override fun clone(): CustomCircularArray<T> = CustomCircularArray(this)

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        private val index: AtomicInteger = AtomicInteger(0)

        override fun hasNext(): Boolean = index.get() < size

        override fun next(): T = get(index.getAndIncrement())
    }

}

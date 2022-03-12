package io.github.devrawr.watcher

import java.io.File
import java.util.*

object Watcher
{
    var interval = 100L

    fun watchFile(
        file: File,
        interval: Long = this.interval,
        action: () -> Unit,
    )
    {
        if (file.isDirectory)
        {
            throw IllegalArgumentException("Provided directory, must be file. Call watchDirectory() instead.")
        }

        val watcher = Timer()
        var lastModified = file.lastModified()

        watcher.scheduleAtFixedRate(
            wrapTask {
                if (file.lastModified() > lastModified)
                {
                    action.invoke()
                }

                lastModified = file.lastModified()
            }, 0, interval
        )
    }

    fun watchDirectory(
        directory: File,
        recursive: Boolean = false,
        interval: Long = this.interval,
        action: (File) -> Unit,
    )
    {
        if (!directory.isDirectory)
        {
            throw IllegalArgumentException("Provided file, must be directory. Call watchFile() instead.")
        }

        val watcher = Timer()

        val filesModified = hashMapOf<String, Long>()
        var lastModified = directory.lastModified()

        fun walk(): List<File>
        {
            return if (recursive)
            {
                directory.walkTopDown().toList()
            } else
            {
                directory.listFiles()?.toList()
                    ?: throw IllegalStateException("Error while listing file")
            }
        }

        walk().forEach {
            filesModified[it.absolutePath] = it.lastModified()
        }

        watcher.scheduleAtFixedRate(
            wrapTask {
                if (directory.lastModified() > lastModified)
                {
                    walk().forEach {
                        val path = it.absolutePath
                        val fileModified = it.lastModified()

                        if (!filesModified.containsKey(path) || filesModified[path] != fileModified)
                        {
                            action.invoke(it)
                        }

                        filesModified[path] = fileModified
                    }
                }

                lastModified = directory.lastModified()
            }, 0, interval
        )
    }

    private fun wrapTask(action: () -> Unit): TimerTask
    {
        return object : TimerTask()
        {
            override fun run()
            {
                action.invoke()
            }
        }
    }
}
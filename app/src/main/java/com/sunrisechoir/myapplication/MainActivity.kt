package com.sunrisechoir.myapplication

import android.Manifest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import com.sunrisechoir.graphql.ProcessMutation
import com.sunrisechoir.graphql.ThreadsQuery
import com.sunrisechoir.patchql.PatchqlApollo

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
        )

        val dbPath = this.getDatabasePath("db.sqlite").absolutePath
        val offsetlogPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path + "/out.offset"
        val pubKey = "@U5GvOKP/YUza9k53DSXxT0mk3PIrnyAmessvNfZl5E0=.ed25519"
        val privateKey = "123abc==.ed25519"

        val apolloPatchql = PatchqlApollo()
        apolloPatchql.new(
            offsetLogPath = offsetlogPath,
            databasePath = dbPath,
            publicKey = pubKey,
            privateKey = privateKey
        )

        val query = ProcessMutation.builder().chunkSize(10).build()
        val threadsQuery = ThreadsQuery.builder().build()

        apolloPatchql.query(query) { res -> println(res.getOrNull()?.data()) }
        apolloPatchql.query(threadsQuery) { res -> println(res.getOrNull()?.data()) }
    }
}

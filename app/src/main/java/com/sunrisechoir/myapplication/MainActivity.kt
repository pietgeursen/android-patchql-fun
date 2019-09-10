package com.sunrisechoir.myapplication

import android.Manifest


import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import com.sunrisechoir.rnpatchql.Patchql
import com.sunrisechoir.rngraphql.ProcessMutation
import com.sunrisechoir.rngraphql.ThreadsQuery

import java.util.Collections
import okio.Buffer
import com.apollographql.apollo.response.ScalarTypeAdapters
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer
import com.apollographql.apollo.internal.json.*
import com.apollographql.apollo.response.OperationResponseParser
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response


import java.math.BigDecimal
import com.beust.klaxon.Parser
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonArray
import com.beust.klaxon.jackson.jackson

class ApolloPatchql(private val patchql: Patchql) {

    private val scalarTypeAdapters = ScalarTypeAdapters(Collections.emptyMap())
    private val jsonParser = Parser.jackson()
    private val normalizer =
        ResponseNormalizer.NO_OP_NORMALIZER as ResponseNormalizer<MutableMap<String, Any>>

    fun query(operation: Operation<*, *, *>, cb: (Result<Response<*>>) -> Unit) {
        val queryString = marshalOperation(operation)
        patchql.query(queryString) { result ->
            cb(result.map { resultString ->
                unMarshalOperation(resultString, operation)
            })
        }
    }

    private fun mapJsonNumbersToBigDecimal(jsn: JsonObject): Map<String, Any?> {
        return jsn.mapValues({ entry ->
            when (val v = entry.value) {
                is Int -> BigDecimal(v)
                is Double -> BigDecimal(v)
                is Long -> BigDecimal(v)
                is JsonArray<*> -> v.map { item -> mapJsonNumbersToBigDecimal(item as JsonObject) }
                is JsonObject -> mapJsonNumbersToBigDecimal(v)
                else -> v
            }
        })
    }

    private fun <D1 : Operation.Data, D2, V1 : Operation.Variables> marshalOperation(query: Operation<D1, D2, V1>): String {

        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.serializeNulls = true
        jsonWriter.beginObject()
        jsonWriter.name("operationName").value(query.name().name())
        jsonWriter.name("variables").beginObject()
        query.variables().marshaller()
            .marshal(InputFieldJsonWriter(jsonWriter, scalarTypeAdapters))
        jsonWriter.endObject()

        jsonWriter.name("query").value(query.queryDocument().replace("\\n", ""))

        jsonWriter.endObject()
        jsonWriter.close()
        return buffer.readByteString().utf8()
    }

    private fun unMarshalOperation(response: String, operation: Operation<*, *, *>): Response<*> {

        val mapper = operation.responseFieldMapper()
        val parser = OperationResponseParser(
            operation, mapper, scalarTypeAdapters,
            normalizer
        )

        val json = jsonParser.parse(response.reader()) as JsonObject
        val mappedJson = mapJsonNumbersToBigDecimal(json)

        return parser.parse(mappedJson)
            .toBuilder()
            .build()
    }

}

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

        val patchql = Patchql()
        patchql.new(
            offsetLogPath = offsetlogPath,
            databasePath = dbPath,
            publicKey = pubKey,
            privateKey = privateKey
        )
        val apolloPathcql = ApolloPatchql(patchql)

        val query = ProcessMutation.builder().chunkSize(10).build()
        val threadsQuery = ThreadsQuery.builder().build()

        apolloPathcql.query(query) { res -> println(res.getOrNull()?.data()) }
        apolloPathcql.query(threadsQuery) { res -> println(res.getOrNull()?.data()) }

    }
}

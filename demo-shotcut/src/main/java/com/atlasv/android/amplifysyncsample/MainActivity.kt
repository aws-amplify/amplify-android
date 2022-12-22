package com.atlasv.android.amplifysyncsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.datastore.generated.model.VFX
import com.amplifyframework.kotlin.core.Amplify
import com.atlasv.android.amplifysyncsample.AmplifyHelper.modelProvider
import com.atlasv.android.amplifysyncsample.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonVfx.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                Amplify.API.query(
                    ModelQuery.list(VFX::class.java)
                )
            }
        }
        binding.buttonAll.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                AmplifyHelper.component.syncFromRemote(
                    grayRelease = 0,
                    dbInitTime = 0,
                    "pt"
                )
            }
        }
        binding.buttonQueryLocal.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                modelProvider.models().forEach {
                    AmplifyHelper.component.storage.query(it, Where.matchesAll())
                }
            }
        }
    }
}
package com.example.catfacts

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.catfacts.model.CatFact
import com.example.catfacts.ui.theme.CatFactsTheme
import com.example.catfacts.viewmodel.AppViewModel
import com.example.catfacts.worker.CatFactsWorker
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.delay

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private lateinit var receiver: BroadcastReceiver
    private lateinit var observer: Observer<WorkInfo>
    private lateinit var workInfo: LiveData<WorkInfo>
    private lateinit var navController: NavHostController
    private val request = OneTimeWorkRequest.Builder(CatFactsWorker::class.java).build()


    @SuppressLint("InlinedApi")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatFactsTheme(content = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    navController = rememberNavController()
                    MainScreen(navController, viewModel, request)
                }
            }
            )
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val catFacts = Gson().fromJson(
                    intent?.getStringExtra("catFacts"),
                    Array<CatFact>::class.java
                ).toList()
                viewModel.updateCatFacts(catFacts)
                navController.navigate("result")
            }
        }

        registerReceiver(receiver, IntentFilter("com.example.catfacts"), Context.RECEIVER_EXPORTED)


        observer = Observer { workInfo ->
            if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                val catFacts = Gson().fromJson(
                    workInfo.outputData.getString("catFacts"),
                    Array<CatFact>::class.java
                ).toList()
                viewModel.updateCatFacts(catFacts)
                navController.navigate("result")
            }
        }

        val workManager = WorkManager.getInstance(this)
        workInfo = workManager.getWorkInfoByIdLiveData(request.id)
        workInfo.observe(this, observer)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        workInfo.removeObserver(observer)
    }
}


@Composable
fun ChooseScreen(viewModel: AppViewModel, request: OneTimeWorkRequest) {
    val context = LocalContext.current
    val timerValue by viewModel.timer.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.select_label),
            fontSize = 25.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.startService(context) },
            modifier = Modifier.width(150.dp)
        ) {
            Text(text = stringResource(R.string.service_button))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.startWorkManager(context, request) },
            modifier = Modifier.width(150.dp)
        ) {
            Text(text = stringResource(R.string.worker_button))
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (timerValue > 0) {
            Text(
                text = "Секунд до перехода: ${(5 - timerValue).toString()}",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 25.sp
            )
        }
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavHostController, viewModel: AppViewModel) {
    val catFacts by viewModel.catFacts.observeAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cat_facts),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopTimer()
                        navController.navigateUp()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },

        ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.cat_facts))
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                itemsIndexed(catFacts) { _, catFact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    ) {
                        Text(
                            text = catFact.fact,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: AppViewModel,
    request: OneTimeWorkRequest
) {
    NavHost(navController = navController, startDestination = "choose") {
        composable("choose") {
            ChooseScreen(viewModel = viewModel, request)
        }
        composable("result") {
            ResultScreen(viewModel = viewModel, navController = navController)
        }
    }
}

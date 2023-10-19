package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService
    lateinit var userNotFoundText: TextView
    lateinit var userNotFoundImage: ImageView
    lateinit var emptyImage: ImageView
    lateinit var emptyText: TextView
    lateinit var resultLabel: TextView
    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        setupListeners()
        showUserName()
        setupRetrofit()
        getAllReposByUserName()
    }

    // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    fun setupView() {
        listaRepositories = findViewById(R.id.rv_list_repositories)
        btnConfirmar = findViewById(R.id.btn_confirm)
        nomeUsuario = findViewById(R.id.et_username)
        progressBar = findViewById(R.id.pb_loading)
        resultLabel = findViewById(R.id.tv_result)
        userNotFoundText = findViewById(R.id.tv_user_404)
        userNotFoundImage = findViewById(R.id.iv_user_404)
        emptyImage = findViewById(R.id.iv_empty)
        emptyText = findViewById(R.id.tv_empty)
    }

    //metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            saveUserLocal()
            getAllReposByUserName()
        }
    }


    // salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private fun saveUserLocal() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.username), nomeUsuario.text.toString())
            apply()
        }
    }

    private fun showUserName() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val result = sharedPref.getString(getString(R.string.username), "")

        if (result != "") {
            nomeUsuario.setText(result)
        }
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        githubApi = retrofit.create(GitHubService::class.java)
    }

    //Metodo responsavel por buscar todos os repositorios do usuario fornecido
    fun getAllReposByUserName() {
        val user = nomeUsuario.text.toString()
        if (user != "") {
            progressBar.isVisible = true
            resultLabel.isVisible = false
            listaRepositories.isVisible = false

            githubApi.getAllRepositoriesByUser(user).enqueue(object : Callback<List<Repository>> {
                override fun onResponse(
                    call: Call<List<Repository>>,
                    response: Response<List<Repository>>
                ) {
                    if (response.isSuccessful) {
                        progressBar.isVisible = false
                        response.body()?.let {
                            setupAdapter(it)
                        }
                    } else {
                        progressBar.isVisible = false
                        resultLabel.isVisible = false
                        listaRepositories.isVisible = false
                        emptyImage.isVisible = false
                        emptyText.isVisible = false
                        userNotFoundImage.isVisible = true
                        userNotFoundText.isVisible = true
                    }
                }

                override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                    Toast.makeText(
                        applicationContext, "Erro", Toast.LENGTH_LONG
                    ).show()
                    progressBar.isVisible = false

                }
            })
        }
    }

    // Metodo responsavel por realizar a configuracao do adapter
    fun setupAdapter(list: List<Repository>) {
        if (list.isEmpty()) {
            resultLabel.isVisible = false
            listaRepositories.isVisible = false
            emptyImage.isVisible = true
            emptyText.isVisible = true
            userNotFoundImage.isVisible = false
            userNotFoundText.isVisible = false
        } else {
            val adapter = RepositoryAdapter(list)
            adapter.btnShareListener = {shareRepositoryLink(it)}
            adapter.repositoryItemListener = {openBrowser(it.htmlUrl) }
            listaRepositories.adapter = adapter
            listaRepositories.addItemDecoration(MarginDecoration(12))
            resultLabel.isVisible = true
            listaRepositories.isVisible = true
            emptyImage.isVisible = false
            emptyText.isVisible = false
            userNotFoundImage.isVisible = false
            userNotFoundText.isVisible = false
        }
    }


    // Metodo responsavel por compartilhar o link do repositorio selecionado
    // @Todo 11 - Colocar esse metodo no click do share item do adapter
    fun shareRepositoryLink(repository: Repository) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Dê uma olhada no repositório ${repository.name} de ${nomeUsuario.text}: ${repository.htmlUrl}"
            )
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio

    // @Todo 12 - Colocar esse metodo no click item do adapter
    fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )

    }

}
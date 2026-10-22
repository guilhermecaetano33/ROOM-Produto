package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration()
            .build()
        productDao = db.productDao()

        binding.btnInserirProduto.setOnClickListener { inserirProduto() }
        binding.btnListarProdutos.setOnClickListener { listarProdutos() }
        binding.btnBuscar.setOnClickListener { buscarPorNome() }
        binding.btnExcluirTodos.setOnClickListener { excluirTodos() }
        binding.btnExcluirPorId.setOnClickListener { excluirPorId() }
        binding.btnAtualizarPreco.setOnClickListener { atualizarPreco() }
    }


    private fun inserirProduto() {
        val name = binding.editProductName.text.toString().trim()
        val price = binding.editProductPrice.text.toString().toDoubleOrNull()
        val quantity = binding.editProductQuantity.text.toString().toIntOrNull() ?: 0
        val description = binding.editProductDescription.text.toString().trim()

        if (name.isEmpty()) {
            binding.editProductName.error = "Informe o nome"
            return
        }

        if (price == null || price <= 0.0) {
            binding.editProductPrice.error = "Informe um preço válido"
            return
        }

        val product = Product(
            name = name,
            price = price,
            description = description.ifEmpty { null },
            quantity = quantity
        )

        lifecycleScope.launch {
            productDao.insertAll(product)
            Log.i("TAG", "Produto inserido: $name - ${moeda(price)}")
            Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()
            limparCampos() // A1
        }
    }


    private fun limparCampos() {
        binding.editProductName.text.clear()
        binding.editProductPrice.text.clear()
        binding.editProductQuantity.text.clear()
        binding.editProductDescription.text.clear()
        binding.editProductName.requestFocus()
    }


    private fun listarProdutos() {
        lifecycleScope.launch {
            val products = productDao.getAll()
            products.forEach {
                Log.i("TAG", "Produto = ${it.name}, Preço = ${moeda(it.price)}, Qtd = ${it.quantity}, Descrição = ${it.description}")
            }
            exibirLista(products)
        }
    }

    private fun exibirLista(products: List<Product>) {
        if (products.isEmpty()) {
            binding.txtResultado.text = "Nenhum produto encontrado."
            return
        }
        val sb = StringBuilder()
        products.forEach {
            sb.appendLine("#${it.id} - ${it.name} | ${moeda(it.price)} | Qtd: ${it.quantity}")
            if (!it.description.isNullOrEmpty()) sb.appendLine("     ${it.description}")
        }

        sb.appendLine()
        sb.appendLine("Produtos cadastrados: ${products.size}")
        sb.append("Soma dos preços: ${moeda(products.sumOf { it.price })}")
        binding.txtResultado.text = sb.toString()
    }


    private fun buscarPorNome() {
        val texto = binding.editBusca.text.toString().trim()
        lifecycleScope.launch {
            exibirLista(productDao.buscarPorNome("%$texto%"))
        }
    }


    private fun excluirTodos() {
        lifecycleScope.launch {
            productDao.deleteAll()
            Toast.makeText(this@MainActivity, "Todos os produtos foram excluídos", Toast.LENGTH_SHORT).show()
            binding.txtResultado.text = ""
        }
    }

    private fun excluirPorId() {
        val id = binding.editProductId.text.toString().toIntOrNull()
        if (id == null) {
            binding.editProductId.error = "Informe o ID"
            return
        }
        lifecycleScope.launch {
            val product = productDao.getById(id)
            if (product == null) {
                Toast.makeText(this@MainActivity, "Produto não encontrado", Toast.LENGTH_SHORT).show()
            } else {
                productDao.delete(product)
                Toast.makeText(this@MainActivity, "Produto excluído", Toast.LENGTH_SHORT).show()
                exibirLista(productDao.getAll())
            }
        }
    }


    private fun atualizarPreco() {
        val id = binding.editProductId.text.toString().toIntOrNull()
        val novoPreco = binding.editNovoPreco.text.toString().toDoubleOrNull()

        if (id == null) {
            binding.editProductId.error = "Informe o ID"
            return
        }
        if (novoPreco == null || novoPreco <= 0.0) {
            binding.editNovoPreco.error = "Informe um preço válido"
            return
        }
        lifecycleScope.launch {
            val product = productDao.getById(id)
            if (product == null) {
                Toast.makeText(this@MainActivity, "Produto não encontrado", Toast.LENGTH_SHORT).show()
            } else {
                productDao.update(product.copy(price = novoPreco))
                Toast.makeText(this@MainActivity, "Preço atualizado!", Toast.LENGTH_SHORT).show()
                binding.editNovoPreco.text.clear()
                exibirLista(productDao.getAll())
            }
        }
    }

    private fun moeda(valor: Double): String =
        "R$ " + String.format(Locale("pt", "BR"), "%.2f", valor)
}

package com.hank.bingo

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
import com.firebase.ui.auth.AuthUI.IdpConfig.GoogleBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hank.bingo.databinding.ActivityMainBinding
import java.util.Arrays

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {
    private val TAG: String? = MainActivity::class.java.simpleName
    private val requestAuth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
        }
    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_signout -> {
                FirebaseAuth.getInstance().signOut()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        FirebaseAuth.getInstance().addAuthStateListener(this)
        super.onStart()
    }

    override fun onStop() {
        FirebaseAuth.getInstance().removeAuthStateListener(this)
        super.onStop()
    }

    fun setFab(view: View) {
        finish()
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        auth.currentUser?.also {
            Log.d(TAG, "auth-uid- ${it.uid}")
            it.displayName?.run {
                FirebaseDatabase.getInstance().getReference("users")
                    .child(it.uid)
                    .child("displayName")
                    .setValue(this)
                    .addOnCompleteListener {
                        Log.d(
                            TAG, "auth-comp- ${auth.currentUser!!.displayName}"
                        )
                    }
            }
//
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val member = snapshot.getValue(Member::class.java)
                        member?.nickname?.also { nick ->
                            binding.tvNickname.setText(nick)
                            Log.d(TAG, "auth-nick- ${nick}")
                        } ?: showNickDialog(it)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        } ?: signUp()

    }

    private fun showNickDialog(user: FirebaseUser) {
        val editText = EditText(this)
        editText.setText("${user.displayName}-")
        AlertDialog.Builder(this)
            .setTitle("Nick Name")
            .setMessage("Enter Nick Name")
            .setView(editText)
            .setPositiveButton("OK") { nick, which ->
                FirebaseDatabase.getInstance().getReference("users")
                    .child(user.uid)
                    .child("nickname")
                    .setValue("${editText.text}")
                Log.d(TAG, "auth-dialog- ${editText.text}")
            }.show()
    }

    //
    private fun signUp() {
        Log.d(TAG, "auth-signUp- ")
        val signIn = AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
            Arrays.asList(
                EmailBuilder().build(),
                GoogleBuilder().build(),
            )
        ).setIsSmartLockEnabled(false).build()
        requestAuth.launch(signIn)
    }

}
package com.hank.bingo

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
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

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, View.OnClickListener {
    var member: Member? = null
    val avatarIds = intArrayOf(
        R.drawable.avatar_0,
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6
    )
    private lateinit var avatar: ImageView
    private lateinit var groupAvatars: Group
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
        avatar = binding.avatar
        groupAvatars = binding.groupAvatars

        groupAvatars.visibility = View.GONE
        avatar.setOnClickListener {
            groupAvatars.visibility =
                if (groupAvatars.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        binding.avatar0.setOnClickListener(this)
        binding.avatar1.setOnClickListener(this)
        binding.avatar2.setOnClickListener(this)
        binding.avatar3.setOnClickListener(this)
        binding.avatar4.setOnClickListener(this)
        binding.avatar5.setOnClickListener(this)
        binding.avatar6.setOnClickListener(this)

    }

    fun tvNickname(view: View) {
        Log.d(TAG, "bingo-tvNickname- ")
        FirebaseAuth.getInstance().currentUser?.let {
            showNickDialog(it.uid, binding.tvNickname.text.toString())
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
        val roomText = EditText(this)
        roomText.setText("Welcome")
        AlertDialog.Builder(this)
            .setTitle("Game Room")
            .setMessage("Room Title?")
            .setView(roomText)
            .setPositiveButton("OK") { dialog, which ->
                val room = GameRoom(roomText.text.toString(), member)
                FirebaseDatabase.getInstance().getReference("rooms")
                    .push().setValue(room)

            }.show()
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        auth.currentUser?.also {
            Log.d(TAG, "auth-uid- ${it.uid}")
            it.displayName?.run {
                FirebaseDatabase.getInstance()
                    .getReference("users")
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
                        member = snapshot.getValue(Member::class.java)
                        member?.nickname?.also { nick ->
                            binding.tvNickname.setText(nick)
                            Log.d(TAG, "auth-nick- ${nick}")
                        } ?: showNickDialog(it)
                        //
                        member?.let {
                            avatar.setImageResource(avatarIds[it.avatarId])
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        } ?: signUp()

    }

    private fun showNickDialog(user: FirebaseUser) {
        val uid = user.uid
        val nick = user.displayName
        showNickDialog(uid, nick)
    }

    private fun showNickDialog(uid: String, nick: String?) {
        val editText = EditText(this)
        editText.setText(nick)
        AlertDialog.Builder(this)
            .setTitle("Nickname")
            .setMessage("Your nickname?")
            .setView(editText)
            .setPositiveButton("OK") { dialog, which ->
                FirebaseDatabase.getInstance().getReference("users")
                    .child(uid)
                    .child("nickname")
                    .setValue("${editText.text.toString()}")
                Log.d(TAG, "bingo-data-dialog- ${editText.text.toString()}")
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

    override fun onClick(v: View?) {
        val selectedId = when (v!!.id) {
            R.id.avatar_0 -> 0
            R.id.avatar_1 -> 1
            R.id.avatar_2 -> 2
            R.id.avatar_3 -> 3
            R.id.avatar_4 -> 4
            R.id.avatar_5 -> 5
            R.id.avatar_6 -> 6
            else -> 0
        }
        FirebaseDatabase.getInstance().getReference("users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child("avatarId")
            .setValue(selectedId)
        groupAvatars.visibility = View.GONE
    }

}
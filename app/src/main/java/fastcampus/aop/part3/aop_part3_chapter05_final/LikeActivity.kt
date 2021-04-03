package fastcampus.aop.part3.aop_part3_chapter05_final

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.yuyakaido.android.cardstackview.*

class LikeActivity : AppCompatActivity(), CardStackListener {

    private lateinit var usersDb: DatabaseReference
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val adapter = CardStackAdapter()
    private val cardItems = mutableListOf<CardItem>()

    private val manager by lazy { CardStackLayoutManager(this, this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        usersDb = FirebaseDatabase.getInstance().reference.child("Users")


        val currentUserDB = usersDb.child(getCurrentUserID())
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("name").value == null) {
                    showNameInputPopup()
                    return
                }

                getUnSelectedUsers()

            }

            override fun onCancelled(error: DatabaseError) {}

        })


        val cardStackView = findViewById<CardStackView>(R.id.cardStackView)


        manager.setStackFrom(StackFrom.Top)
        manager.setTranslationInterval(8.0f)
        manager.setSwipeThreshold(0.1f)

        cardStackView.layoutManager = manager
        cardStackView.adapter = adapter

    }

    private fun getCurrentUserID(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        return auth.currentUser.uid
    }

    fun getUnSelectedUsers() {
        usersDb.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.child("userId").value != getCurrentUserID()
                    && dataSnapshot.child("likedBy").child("like").hasChild(getCurrentUserID()).not()
                    && dataSnapshot.child("likedBy").child("disLike").hasChild(getCurrentUserID()).not()
                ) {

                    val userId = dataSnapshot.child("userId").value.toString()
                    var name = "undecided"
                    if (dataSnapshot.child("name").value != null) {
                        name = dataSnapshot.child("name").value.toString()
                    }

                    cardItems.add(CardItem(userId, name))
                    adapter.submitList(cardItems)
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun showNameInputPopup() {
        val editText = EditText(this)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("이름을 입력해주세요")
            .setView(editText)
            .setPositiveButton("확인") { _, _ ->
                if (editText.text.isEmpty()) {
                    showNameInputPopup()
                } else {
                    saveUserName(editText.text.toString())
                }

            }
            .setCancelable(false)
            .show()

    }

    private fun saveUserName(name: String) {
        val userId: String = getCurrentUserID()
        val currentUserDb = usersDb.child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        user["name"] = name
        currentUserDb.updateChildren(user)

        getUnSelectedUsers()
    }

    private fun like() {
        val card = cardItems[manager.topPosition - 1]

        usersDb.child(card.userId)
            .child("likedBy")
            .child("like")
            .child(getCurrentUserID())
            .setValue(true)

        Toast.makeText(this, "Like 하셨습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun disLike() {
        val card = cardItems[manager.topPosition - 1]

        usersDb.child(card.userId)
            .child("likedBy")
            .child("disLike")
            .child(getCurrentUserID())
            .setValue(true)

        Toast.makeText(this, "DisLike 하셨습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}

    override fun onCardSwiped(direction: Direction?) {
        when (direction) {
            Direction.Right -> like()
            Direction.Left -> disLike()
            else -> {}
        }
    }

    override fun onCardRewound() {}

    override fun onCardCanceled() {}

    override fun onCardAppeared(view: View?, position: Int) {}

    override fun onCardDisappeared(view: View?, position: Int) {}
}
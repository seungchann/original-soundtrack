package com.example.assign2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.assign2.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.KakaoSdkError
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .build()
    val Req_Code: Int = 123

    private lateinit var binding: ActivityMainBinding
    private lateinit var mCallback: (OAuthToken?, Throwable?) -> Unit
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    lateinit var currentUser: Member
    private val retrofitService = RetrofitService.getInstance()
    lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        makeKakaoCallback()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        kakaoButton.setOnClickListener {
            checkKakaoLogin()
        }

        myButton.setOnClickListener {
            googleSignIn()
        }

        viewModel = ViewModelProvider(this, LoginViewModelFactory(MainRepository(retrofitService))).get(LoginViewModel::class.java)
        viewModel.errorMessage.observe(this, Observer { })
        viewModel.getAllMembers()
    }

    private fun makeKakaoCallback() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) { Log.e(TAG, "????????? ??????", error) }
            else if (token != null) {
                Log.i(TAG, "????????? ?????? ${token.accessToken}")
                UserApiClient.instance.me { user, error ->
                    if (error != null) { Log.e(TAG, "????????? ?????? ?????? ??????", error) }
                    else if (user != null) { makeMemberFromKakaoDB(user) }
                }
            }
        }
        mCallback = callback
    }

    private fun checkKakaoLogin() {
        if (AuthApiClient.instance.hasToken()) {
            UserApiClient.instance.accessTokenInfo { _, error ->
                if (error != null) {
                    if (error is KakaoSdkError && error.isInvalidTokenError() == true) { KakaoLogin() }
                    else { Log.e("Kakao_Token", "?????? ??????") } //?????? ??????
                }
                else {
                    Log.e("?????????","???????????????.")
                    //?????? ????????? ?????? ??????(?????? ??? ?????? ?????????)
                    UserApiClient.instance.me { user, error ->
                        if (error != null) { Log.e(TAG, "????????? ?????? ?????? ??????", error) }
                        else if (user != null) { makeMemberFromKakaoDB(user) }
                    }
                }
            }
        }
        else { KakaoLogin() }
    }

    // ??????????????? ???????????? ?????? ??????, ???????????? ??????
    // ????????? ?????? ??????, ????????? ???????????? ?????????
    fun KakaoLogin() {
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this, callback = mCallback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = mCallback)
        }
    }

    fun makeMemberFromKakaoDB(user: User) {
        Log.i(TAG, "????????? ?????? ?????? ??????" +
                "\n?????????: ${user.kakaoAccount?.email}" +
                "\n?????????: ${user.kakaoAccount?.profile?.nickname}" +
                "\n???????????????: ${user.kakaoAccount?.profile?.thumbnailImageUrl}")

        val newMember = Member(user.kakaoAccount?.profile?.nickname ?: "default",
            user.kakaoAccount?.email ?: "@.",
            user.kakaoAccount?.profile?.thumbnailImageUrl ?: "",
            0)

        currentUser = newMember
        moveToStartActivity()
    }

    fun googleSignIn() {
        var signInIndent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIndent, Req_Code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Req_Code) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            var account = completedTask.getResult(ApiException::class.java)

            Log.i(TAG, "????????? ?????? ?????? ??????" +
                    "\n?????????: ${account?.email.toString()}" +
                    "\n?????????: ${account?.displayName.toString()}" +
                    "\n???????????????: ${account?.photoUrl.toString()}")

            val newMember = Member(account?.displayName.toString() ?: "default",
                account?.email.toString() ?: "@.",
                account?.photoUrl.toString() ?: "",
                0)

            currentUser = newMember
            checkCurrentUserFromDB()
            moveToStartActivity()

        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("failed", "signInResult:failed code=" + e.statusCode)
        }
    }

    fun checkCurrentUserFromDB() {
        viewModel.memberList.observe(this, Observer {
            val memberList: MutableList<Member> = it.toMutableList()
            var hasMemberAccountOnDB: Boolean = false
            memberList.forEach {
                if (currentUser.email == it.email) {
                    hasMemberAccountOnDB = true
                    currentUser.highestScore = it.highestScore
                }
            }
            if (!hasMemberAccountOnDB) {
                Log.e("hasMemberAccountOnDB", "?????????????????????")
                viewModel.addMemeber(currentUser)
            }
        })
    }

    fun moveToStartActivity() {
        val nextIntent = Intent(this, StartActivity::class.java)
        nextIntent.putExtra("currentUser",currentUser)
        checkCurrentUserFromDB()
        startActivity(nextIntent)
    }
}
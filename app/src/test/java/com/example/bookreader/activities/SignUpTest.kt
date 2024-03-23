package com.example.bookreader.activities

import android.content.Intent
import android.widget.Toast
import com.example.bookreader.activities.SignInActivity
import com.example.bookreader.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SignUpTest {

    @Mock
    private lateinit var mockBinding: ActivitySignUpBinding

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockToast: Toast

    private lateinit var signUpActivity: SignUpActivity

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        signUpActivity = SignUpActivity()
        signUpActivity.binding = mockBinding
        signUpActivity.firebaseAuth = mockFirebaseAuth
    }

    @Test
    fun testEmptyFields() {
        // Symulowanie pustych pól e-maila, hasła i potwierdzenia hasła
        `when`(mockBinding.emailEt.text.toString()).thenReturn("")
        `when`(mockBinding.passET.text.toString()).thenReturn("")
        `when`(mockBinding.confirmPassEt.text.toString()).thenReturn("")

        signUpActivity.onCreate(null)

        // Upewnienie się, że użytkownik otrzymuje odpowiednie ostrzeżenie o konieczności uzupełnienia pól
        assertEquals("Proszę uzupełnić pola!", mockToast.text)
    }

    @Test
    fun testNumericPassword() {
        // Symulowanie wprowadzenia samego ciągu cyfr jako hasła
        `when`(mockBinding.emailEt.text.toString()).thenReturn("example@example.com")
        `when`(mockBinding.passET.text.toString()).thenReturn("123456")
        `when`(mockBinding.confirmPassEt.text.toString()).thenReturn("123456")

        signUpActivity.onCreate(null)

        // Upewnienie się, że użytkownik otrzymuje odpowiednie ostrzeżenie o zbyt prostym haśle
        assertEquals("Hasło musi zawierać przynajmniej jedną literę.", mockToast.text)
    }
}

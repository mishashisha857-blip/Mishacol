package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserProfileEntity
import com.example.data.sensor.StepSensorManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NutriFit AI", appName)
  }

  @Test
  fun `verify user profile bmr and tdee calculations`() {
    val profile = UserProfileEntity(
      currentWeightKg = 80f,
      targetWeightKg = 72f,
      heightCm = 180f,
      age = 30,
      gender = "MALE",
      deficitTargetKcal = 400
    )
    assertTrue(profile.bmr > 1500f)
    assertTrue(profile.targetDailyCalories > 1200)
    assertTrue(profile.targetProteinGrams > 50f)
  }

  @Test
  fun `verify step calorie calculation`() {
    val calories = StepSensorManager.calculateBurnedCalories(10000, 70f)
    assertTrue(calories in 350..450)
  }
}

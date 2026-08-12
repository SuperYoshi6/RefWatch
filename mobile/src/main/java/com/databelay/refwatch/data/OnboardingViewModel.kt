package com.databelay.refwatch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel // <-- ADD THIS ANNOTATION
class OnboardingViewModel @Inject constructor( // <-- ADD @Inject
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState(steps = OnboardingStepsProvider.create(context)))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
         val hasCompleted = prefs.getBoolean("onboarding_completed", false)
        // -----------------------------------------------------------

        if (hasCompleted) {
            _uiState.update { it.copy(currentStepIndex = it.steps.size) }
        }
    }

    fun advanceTour() {
        viewModelScope.launch {
            val currentIndex = _uiState.value.currentStepIndex
            val totalSteps = _uiState.value.steps.size

            if (currentIndex == totalSteps - 1) {
                prefs.edit().putBoolean("onboarding_completed", true).apply()
            }

            if (currentIndex < totalSteps) {
                _uiState.update { it.copy(currentStepIndex = currentIndex + 1) }
            }
        }
    }

    fun dismissTour() {
        viewModelScope.launch {
            prefs.edit().putBoolean("onboarding_completed", true).apply()
            _uiState.update { it.copy(currentStepIndex = it.steps.size) }
        }
    }
}

data class OnboardingUiState @OptIn(ExperimentalMaterial3Api::class) constructor(
    val steps: List<OnboardingStep>,
    val currentStepIndex: Int = 0
) {
    val isTourActive: Boolean
        get() = currentStepIndex < steps.size
}

/**
 * Factory for [OnboardingUiState] that resolves localized strings from string resources.
 * Use this from the ViewModel so the data class stays free of Context.
 */
object OnboardingStepsProvider {
    @OptIn(ExperimentalMaterial3Api::class)
    fun create(context: Context): List<OnboardingStep> = listOf(
        OnboardingStep(
            title = context.getString(R.string.onb_create_new_game_title),
            message = context.getString(R.string.onb_create_new_game_msg),
            targetTag = "add_game_fab",
            nextButtonLabel = context.getString(R.string.onb_next)
        ),
        OnboardingStep(
            title = context.getString(R.string.onb_import_calendar_title),
            message = context.getString(R.string.onb_import_calendar_msg),
            targetTag = "import_ics_button",
            nextButtonLabel = context.getString(R.string.onb_next)
        ),
        OnboardingStep(
            title = context.getString(R.string.onb_signout_title),
            message = context.getString(R.string.onb_signout_msg),
            targetTag = "sign_out_button",
            nextButtonLabel = context.getString(R.string.onb_finish)
        ),
    )
}


// A data class to hold the details for each step in our onboarding tour.
//@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
data class OnboardingStep(
    val title: String,
    val message: String,
    val targetTag: String,
    val nextButtonLabel: String = "",
    val tooltipState: TooltipState = TooltipState(isPersistent = true, initialIsVisible = false)
)


/**
 * A wrapper composable that shows a tooltip for its content.
 * This encapsulates the TooltipBox logic for a single onboarding step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationArea(
    tag: String, // The unique tag for the content being wrapped
    onboardingStep: OnboardingStep?, // Takes the whole step object
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    // Check if the current step is active and targets this specific component
    val isStepActiveForThisComponent = onboardingStep?.targetTag == tag

    if (isStepActiveForThisComponent) {
        // If this is the active component, wrap it in the TooltipBox
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Below,
                16.dp
            ),
            tooltip = {
                RichTooltip(
                    title = { Text(onboardingStep.title) },
                    action = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.game_log_close))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = onNext) {
                                Text(onboardingStep.nextButtonLabel)
                            }
                        }
                    },
                    colors = TooltipDefaults.richTooltipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        actionContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    caretShape = TooltipDefaults.caretShape(DpSize(16.dp, 16.dp)),
                ) {
                    Text(onboardingStep.message)
                }

                /*                OnboardingTooltipContent(
                                    title = onboardingStep.title,
                                    message = onboardingStep.message,
                                    onNext = onNext,
                                    onDismiss = onDismiss,
                                    nextButtonLabel = onboardingStep.nextButtonLabel
                                )*/
            },
            state = onboardingStep.tooltipState,
        ) {
            content() // The wrapped content (e.g., IconButton)
        }
    } else {
        // If this is not the active step, just show the content directly.
        content()
    }
}

/**
 * A custom composable for the content inside the tooltip.
 */
@Composable
fun OnboardingTooltipContent(
    title: String,
    message: String,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    nextButtonLabel: String
) {
    Surface(
        modifier = Modifier.widthIn(max = 250.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.game_log_close))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onNext) {
                    Text(nextButtonLabel)
                }
            }
        }
    }
}

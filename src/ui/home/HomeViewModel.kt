package ai.or4cl3.lumina.ui.home

import ai.or4cl3.lumina.core.offline.LuminaRepository
import ai.or4cl3.lumina.core.session.ChildProfile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LuminaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val profile = repository.getMostRecentProfile()
            if (profile == null) {
                _uiState.value = HomeUiState.NoProfile
                return@launch
            }

            val sessions = repository.getRecentSessions(profile.id, limit = 30)
            val streak   = calculateStreak(sessions.map { it.startedAt })
            val totalMin = sessions.sumOf { it.durationMinutes }
            val totalSessions = repository.getSessionCount(profile.id)

            _uiState.value = HomeUiState.Ready(
                profile       = profile,
                streakDays    = streak,
                totalSessions = totalSessions,
                totalMinutes  = totalMin,
                growthNotes   = buildGrowthNotes(totalSessions, totalMin)
            )
        }
    }

    /** Calculate consecutive days with at least one session. */
    private fun calculateStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val todayDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        val sessionDays = timestamps
            .map { TimeUnit.MILLISECONDS.toDays(it) }
            .toSortedSet()
            .reversed()

        var streak = 0
        var expected = todayDay
        for (day in sessionDays) {
            if (day == expected || day == expected - 1) {
                if (day == expected - 1) expected = day
                streak++
            } else break
        }
        return streak
    }

    /**
     * Build warm, narrative-style growth notes.
     * NO scores, NO grades — only celebration of effort and presence.
     */
    private fun buildGrowthNotes(sessions: Int, minutes: Int): List<String> {
        val notes = mutableListOf<String>()
        if (sessions >= 1) notes += "You've shown up $sessions time${if (sessions > 1) "s" else ""} to learn. That takes courage."
        if (minutes >= 10) notes += "You've spent ${minutes} minutes exploring and growing."
        if (sessions >= 5) notes += "Five sessions complete! You keep coming back. That's what makes the difference."
        if (sessions >= 10) notes += "Ten sessions! You are a real learner."
        if (notes.isEmpty()) notes += "Every journey starts with one step. This is yours."
        return notes
    }
}

// ── UI State ────────────────────────────────────────────────────────────────

sealed class HomeUiState {
    object Loading                                          : HomeUiState()
    object NoProfile                                        : HomeUiState()
    data class Ready(
        val profile       : ChildProfile,
        val streakDays    : Int,
        val totalSessions : Int,
        val totalMinutes  : Int,
        val growthNotes   : List<String>
    ) : HomeUiState()
}

package com.sevapath.app

import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityScorerTest {
    @Test
    fun hotspotOrderingMatchesCitizenNeed() {
        val water = PriorityScorer.score(PriorityFactors(.98, .95, .92, .82, .88, .70, .18))
        val road = PriorityScorer.score(PriorityFactors(.76, .70, .58, .90, .62, .78, .31))
        assertTrue(water > road)
    }

    @Test
    fun newSignupStartsWithNoPersonalData() {
        val viewModel = CivicViewModel()

        assertTrue(viewModel.signup("New Citizen", "new@sevapath.app", "secret1"))
        assertTrue(viewModel.requests.isEmpty())
        assertTrue(viewModel.news.isEmpty())
    }
}

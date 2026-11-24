package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.PlanStatus
import com.dayflow.app.domain.repository.DailyPlanRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class ApproveDailyPlanUseCase @Inject constructor(
    private val dailyPlanRepository: DailyPlanRepository
) {

    suspend operator fun invoke(
        date: LocalDate,
        items: List<DailyPlanItem>
    ): DailyPlan {
        val existingPlan = dailyPlanRepository.getPlanByDateDirect(date)
        val planId = existingPlan?.id ?: UUID.randomUUID().toString()

        val indexedItems = items.mapIndexed { index, item ->
            item.copy(planId = planId, orderIndex = index)
        }

        val approvedPlan = DailyPlan(
            id = planId,
            localDate = date,
            status = PlanStatus.APPROVED,
            createdAt = existingPlan?.createdAt ?: Instant.now(),
            approvedAt = Instant.now(),
            updatedAt = Instant.now(),
            items = indexedItems
        )

        dailyPlanRepository.savePlanWithItems(approvedPlan, indexedItems)
        return approvedPlan
    }
}

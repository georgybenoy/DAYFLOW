package com.dayflow.app.domain.usecase

interface UseCase<in Params, out ReturnType> {
    suspend operator fun invoke(params: Params): ReturnType
}

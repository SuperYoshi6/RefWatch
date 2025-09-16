// In a new file, e.g., RepositoryModule.kt or in your existing AppModule.kt
package com.databelay.refwatch.di // Or your DI package

import com.databelay.refwatch.auth.AuthRepository
import com.databelay.refwatch.auth.FirebaseAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Or ViewModelComponent, ActivityRetainedComponent, etc.
abstract class AuthRepositoryModule {

    @Binds
    @Singleton // Ensure the scope matches the concrete implementation's scope
    abstract fun bindAuthRepository(
        firebaseAuthRepository: FirebaseAuthRepository
    ): AuthRepository
}
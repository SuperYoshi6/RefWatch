package com.databelay.refwatch.di
import com.databelay.refwatch.auth.AuthRepository // Import new repository
import com.databelay.refwatch.games.GameStorageMobile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth // Needed for AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent // GameRepository likely a singleton
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton // GameRepository can be a singleton
    fun provideGameRepository(firestore: FirebaseFirestore): GameStorageMobile {
        // Hilt will automatically provide the FirebaseFirestore instance
        // from FirebaseModule because it knows how to create it.
        return GameStorageMobile(firestore)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository = // Provide AuthRepository
        AuthRepository(firebaseAuth)
}
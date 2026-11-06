package br.com.desbloqueia.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive migration: no question, attempt, presentation or unlock state is deleted. */
val MIGRATION_1_2=object: Migration(1,2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE questions ADD COLUMN topicId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_questions_topicId ON questions(topicId)")
        db.execSQL("ALTER TABLE attempts ADD COLUMN topicId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_topicId ON attempts(topicId)")
        db.execSQL("ALTER TABLE unlock_state ADD COLUMN pendingKind TEXT NOT NULL DEFAULT 'question'")
        db.execSQL("CREATE TABLE IF NOT EXISTS flashcards (id TEXT NOT NULL,discipline TEXT NOT NULL,topicId TEXT NOT NULL,fingerprint TEXT NOT NULL,payload TEXT NOT NULL,PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_flashcards_topicId ON flashcards(topicId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_flashcards_fingerprint ON flashcards(fingerprint)")
        db.execSQL("CREATE TABLE IF NOT EXISTS flash_progress (cardId TEXT NOT NULL,correct INTEGER NOT NULL,wrong INTEGER NOT NULL,streak INTEGER NOT NULL,lastReviewAt INTEGER NOT NULL,nextReviewAt INTEGER NOT NULL,state TEXT NOT NULL,PRIMARY KEY(cardId))")
        db.execSQL("CREATE TABLE IF NOT EXISTS flash_reviews (presentationId TEXT NOT NULL,cardId TEXT NOT NULL,topicId TEXT NOT NULL,known INTEGER NOT NULL,reviewedAt INTEGER NOT NULL,dueAt INTEGER NOT NULL,PRIMARY KEY(presentationId))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_flash_reviews_cardId ON flash_reviews(cardId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_flash_reviews_topicId ON flash_reviews(topicId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS flash_presentations (id TEXT NOT NULL,cardId TEXT NOT NULL,shownAt INTEGER NOT NULL,revealed INTEGER NOT NULL,PRIMARY KEY(id))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_flash_presentations_cardId ON flash_presentations(cardId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS study_mix (channel TEXT NOT NULL,credit INTEGER NOT NULL,percent INTEGER NOT NULL,mode TEXT NOT NULL,PRIMARY KEY(channel))")
    }
}

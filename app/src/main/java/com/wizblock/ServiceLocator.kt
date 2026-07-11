package com.wizblock

import android.content.Context
import androidx.room.Room
import com.wizblock.admin.DeviceAdminController
import com.wizblock.browser.DefaultUrlExtractionEngine
import com.wizblock.browser.UrlExtractionEngine
import com.wizblock.data.local.room.AppDatabase
import com.wizblock.data.preferences.DataStoreBlockingStateRepository
import com.wizblock.data.repository.BlockEventRepository
import com.wizblock.data.repository.BlockEventRepositoryImpl
import com.wizblock.data.repository.BlocklistCreator
import com.wizblock.data.repository.BlockingStateRepository
import com.wizblock.data.repository.PolicyRepository
import com.wizblock.data.repository.PolicyRepositoryImpl
import com.wizblock.data.repository.ProfileRepository
import com.wizblock.data.repository.ProfileRepositoryImpl
import com.wizblock.data.repository.RuleRepository
import com.wizblock.data.repository.RuleRepositoryImpl
import com.wizblock.data.repository.ScheduleRepository
import com.wizblock.data.repository.ScheduleRepositoryImpl
import com.wizblock.data.repository.StarterBlocklistSeeder
import com.wizblock.data.repository.UsageCounterRepository
import com.wizblock.data.repository.UsageCounterRepositoryImpl
import com.wizblock.data.repository.UsageLimitRepository
import com.wizblock.data.repository.UsageLimitRepositoryImpl
import com.wizblock.domain.DefaultDomainNormalizer
import com.wizblock.domain.CategoryMatcher
import com.wizblock.domain.DomainNormalizer
import com.wizblock.domain.LocalCategoryPacks
import com.wizblock.domain.TargetDisplayResolver
import com.wizblock.domain.TargetNormalizer
import com.wizblock.overlay.OverlayController
import com.wizblock.overlay.SystemOverlayController
import com.wizblock.policy.PolicyEvaluator
import com.wizblock.policy.ScheduleEvaluator
import kotlinx.coroutines.flow.first

object ServiceLocator {
    @Volatile
    private var initialized = false

    lateinit var database: AppDatabase
        private set
    lateinit var domainNormalizer: DomainNormalizer
        private set
    lateinit var targetNormalizer: TargetNormalizer
        private set
    lateinit var targetDisplayResolver: TargetDisplayResolver
        private set
    lateinit var categoryMatcher: CategoryMatcher
        private set
    lateinit var profileRepository: ProfileRepository
        private set
    lateinit var blocklistCreator: BlocklistCreator
        private set
    lateinit var ruleRepository: RuleRepository
        private set
    lateinit var scheduleRepository: ScheduleRepository
        private set
    lateinit var usageLimitRepository: UsageLimitRepository
        private set
    lateinit var usageCounterRepository: UsageCounterRepository
        private set
    lateinit var blockEventRepository: BlockEventRepository
        private set
    lateinit var blockingStateRepository: BlockingStateRepository
        private set
    lateinit var policyRepository: PolicyRepository
        private set
    lateinit var urlExtractionEngine: UrlExtractionEngine
        private set
    lateinit var overlayController: OverlayController
        private set
    lateinit var deviceAdminController: DeviceAdminController
        private set
    lateinit var scheduleEvaluator: ScheduleEvaluator
        private set
    lateinit var policyEvaluator: PolicyEvaluator
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            val appContext = context.applicationContext
            database = Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                "wizblock_phase2.db"
            ).addMigrations(AppDatabase.MIGRATION_3_4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

            domainNormalizer = DefaultDomainNormalizer()
            targetNormalizer = TargetNormalizer(domainNormalizer)
            targetDisplayResolver = TargetDisplayResolver(appContext, targetNormalizer)
            categoryMatcher = CategoryMatcher(LocalCategoryPacks.load(appContext))
            profileRepository = ProfileRepositoryImpl(database.profileDao())
            ruleRepository = RuleRepositoryImpl(
                dao = database.ruleDao(),
                targetNormalizer = targetNormalizer
            )
            blocklistCreator = BlocklistCreator(
                profileRepository = profileRepository,
                ruleRepository = ruleRepository
            )
            scheduleRepository = ScheduleRepositoryImpl(database.scheduleDao())
            usageLimitRepository = UsageLimitRepositoryImpl(
                dao = database.usageLimitDao(),
                targetNormalizer = targetNormalizer
            )
            usageCounterRepository = UsageCounterRepositoryImpl(
                dao = database.usageCounterDao(),
                targetNormalizer = targetNormalizer
            )
            blockEventRepository = BlockEventRepositoryImpl(database.blockEventDao())
            blockingStateRepository = DataStoreBlockingStateRepository(appContext)
            policyRepository = PolicyRepositoryImpl(
                blockingStateRepository = blockingStateRepository,
                ruleRepository = ruleRepository,
                scheduleRepository = scheduleRepository,
                usageLimitRepository = usageLimitRepository,
                profileRepository = profileRepository
            )
            urlExtractionEngine = DefaultUrlExtractionEngine(domainNormalizer)
            overlayController = SystemOverlayController(appContext)
            deviceAdminController = DeviceAdminController(appContext)
            scheduleEvaluator = ScheduleEvaluator()
            policyEvaluator = PolicyEvaluator(scheduleEvaluator)

            kotlinx.coroutines.runBlocking {
                profileRepository.ensureDefaultProfile()
                StarterBlocklistSeeder(
                    profileRepository = profileRepository,
                    ruleRepository = ruleRepository,
                    isSeeded = { blockingStateRepository.starterBlocklistsSeeded.first() },
                    markSeeded = { blockingStateRepository.setStarterBlocklistsSeeded(true) }
                ).ensureSeeded()
            }

            initialized = true
        }
    }
}

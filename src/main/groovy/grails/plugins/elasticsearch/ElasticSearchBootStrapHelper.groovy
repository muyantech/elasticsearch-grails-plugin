package grails.plugins.elasticsearch

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.plugins.elasticsearch.mapping.MappingMigrationStrategy
import grails.plugins.elasticsearch.util.ElasticSearchConfigAware
import grails.plugins.elasticsearch.util.IndexNamingUtils
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by @marcos-carceles on 13/01/15.
 * Created and exposed as a bean, because Bootstrap cannot be easily tested and invoked from IntegrationSpec
 */
@CompileStatic
class ElasticSearchBootStrapHelper implements ElasticSearchConfigAware {

    private static final Logger LOG = LoggerFactory.getLogger(this)

    GrailsApplication grailsApplication
    ElasticSearchService elasticSearchService
    ElasticSearchAdminService elasticSearchAdminService
    ElasticSearchContextHolder elasticSearchContextHolder

  void bulkIndexOnStartup() {
    Thread.start {
      run()
    }
  }

  @Transactional(readOnly = true)
  void run() {
    def bulkIndexOnStartup = esConfig?.bulkIndexOnStartup
    //Index Content
    if (bulkIndexOnStartup == "deleted") { //Index lost content due to migration
      LOG.debug "Performing bulk indexing of classes requiring index/mapping migration ${elasticSearchContextHolder.indexesRebuiltOnMigration} on their new version."
      Class[] domainsToReindex = elasticSearchContextHolder.findMappedClassesOnIndices(elasticSearchContextHolder.indexesRebuiltOnMigration) as Class[]
      elasticSearchService.index(domainsToReindex)
    } else if (bulkIndexOnStartup) { //Index all
      LOG.debug "Performing bulk indexing."
      elasticSearchService.index(Collections.emptyMap()) // empty map is needed for static compiling
    }
    //Update index aliases where needed
    MappingMigrationStrategy migrationStrategy = migrationConfig?.strategy ? MappingMigrationStrategy.valueOf(migrationConfig?.strategy as String) : MappingMigrationStrategy.none
    if (migrationStrategy == MappingMigrationStrategy.alias) {
      elasticSearchContextHolder.indexesRebuiltOnMigration.each { String indexName ->
        int latestVersion = elasticSearchAdminService.getLatestVersion(indexName)
        if(!migrationConfig?.disableAliasChange) {
          elasticSearchAdminService.pointAliasTo IndexNamingUtils.queryingIndexFor(indexName), indexName, latestVersion
        }
        elasticSearchAdminService.pointAliasTo IndexNamingUtils.indexingIndexFor(indexName), indexName, latestVersion
      }
    }
  }

}

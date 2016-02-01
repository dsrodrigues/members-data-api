package framework

import play.api._
import play.api.cache.EhCacheComponents
import play.api.i18n._
import play.api.libs.concurrent.AkkaComponents
import play.api.libs.openid._
import play.api.libs.ws.ning.NingWSComponents
import play.filters.cors.CORSComponents
import play.filters.csrf.CSRFComponents
import play.filters.gzip.GzipFilterComponents
import play.filters.headers.SecurityHeadersComponents

trait AllComponentTraits extends
      BuiltInComponents with
      EhCacheComponents with
      I18nComponents with
      OpenIDComponents with
      NingWSComponents with
      CORSComponents with
      CSRFComponents with
      GzipFilterComponents with
      SecurityHeadersComponents
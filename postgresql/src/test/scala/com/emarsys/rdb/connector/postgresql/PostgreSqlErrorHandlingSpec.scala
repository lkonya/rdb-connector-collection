package com.emarsys.rdb.connector.postgresql

import java.sql.SQLException

import com.emarsys.rdb.connector.common.models.Errors._
import org.postgresql.util.{PSQLException, PSQLState}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, WordSpec}

class PostgreSqlErrorHandlingSpec extends WordSpec with Matchers with TableDrivenPropertyChecks {

  private val queryTimeoutException              = new SQLException("msg", "57014")
  private val syntaxErrorException               = new PSQLException("msg", PSQLState.SYNTAX_ERROR)
  private val columnNotFoundException            = new SQLException("msg", "42703")
  private val permissionDeniedException          = new SQLException("msg", "42501")
  private val tableNotFoundException             = new SQLException("msg", "42P01")
  private val unableToConnectException           = new PSQLException("", PSQLState.CONNECTION_UNABLE_TO_CONNECT)
  private val invalidAuthorizationException      = new PSQLException("msg", PSQLState.INVALID_AUTHORIZATION_SPECIFICATION)
  private val connectionFailureException         = new PSQLException("msg", PSQLState.CONNECTION_FAILURE)
  private val invalidPasswordException           = new SQLException("msg", "28P01")

  val testCases = Table(
    ("database error", "sqlException", "clientError"),
    ("query timeout", queryTimeoutException, QueryTimeout(queryTimeoutException.getMessage)),
    ("syntax error", syntaxErrorException, SqlSyntaxError(syntaxErrorException.getMessage)),
    ("column not found error", columnNotFoundException, SqlSyntaxError(columnNotFoundException.getMessage)),
    ("permission denied error", permissionDeniedException, AccessDeniedError(permissionDeniedException.getMessage)),
    ("table not found error", tableNotFoundException, TableNotFound(tableNotFoundException.getMessage)),
    ("unable to connect error", unableToConnectException, ConnectionError(unableToConnectException)),
    ("invalid authorization error", invalidAuthorizationException, ConnectionError(invalidAuthorizationException)),
    ("connection failure error", connectionFailureException, ConnectionError(connectionFailureException)),
    ("invalid password error", invalidPasswordException, ConnectionError(invalidPasswordException))
  )

  "PostgreSqlErrorHandling" should {

    forAll(testCases) {
      case (errorType, sqlException, clientError) =>
        s"convert $errorType to ${clientError.getClass.getSimpleName}" in new PostgreSqlErrorHandling {
          eitherErrorHandler().apply(sqlException) shouldEqual Left(clientError)
        }
    }
  }
}

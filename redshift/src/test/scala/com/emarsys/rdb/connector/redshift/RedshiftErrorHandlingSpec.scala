package com.emarsys.rdb.connector.redshift

import java.sql.{SQLException, SQLTransientConnectionException}

import com.emarsys.rdb.connector.common.models.Errors._
import org.scalatest.{Matchers, WordSpecLike}

class RedshiftErrorHandlingSpec extends WordSpecLike with Matchers {

  val possibleSQLErrors = Seq(
    (
      "HY000",
      "Connection is not available, request timed out after",
      ConnectionTimeout("Connection is not available, request timed out after")
    ),
    (
      "HY000",
      "[Amazon](500053) The TCP Socket has timed out while waiting for response",
      QueryTimeout("[Amazon](500053) The TCP Socket has timed out while waiting for response")
    ),
    ("HY000", "other error with HY000", ErrorWithMessage("[HY000] - [999] - other error with HY000")),
    ("57014", "query cancelled", QueryTimeout("query cancelled")),
    ("42601", "sql syntax error", SqlSyntaxError("sql syntax error")),
    ("42501", "permission denied", AccessDeniedError("permission denied")),
    ("42P01", "relation not found", TableNotFound("relation not found")),
    (
      "42702",
      "[Amazon](500310) Invalid operation: column reference \"seller_id\" is ambiguous;",
      SqlSyntaxError("[Amazon](500310) Invalid operation: column reference \"seller_id\" is ambiguous;")
    )
  )

  val possibleConnectionErrors = Seq(
    ("08001", "unable to connect"),
    ("28000", "invalid authorization"),
    ("08006", "server process is terminating"),
    ("28P01", "invalid password")
  )

  private def shouldBeWithCause[T](
      result: Either[ConnectorError, T],
      expected: ConnectorError,
      expectedCause: Throwable
  ): Unit = {
    result shouldBe Left(expected)
    result.left.get.getCause shouldBe expectedCause
  }

  "RedshiftErrorHandling" should {

    possibleSQLErrors.foreach {
      case (sqlState, message, expectedError) =>
        s"""convert $message to ${expectedError.getClass.getSimpleName}""" in new RedshiftErrorHandling {
          val sqlException = new SQLException(message, sqlState, 999)
          eitherErrorHandler.apply(sqlException) shouldEqual Left(expectedError)
          shouldBeWithCause(eitherErrorHandler.apply(sqlException), expectedError, sqlException)
        }
    }

    possibleConnectionErrors.foreach {
      case (sqlState, message) =>
        s"""convert $message to ConnectionError""" in new RedshiftErrorHandling {
          val sqlException = new SQLException("msg", sqlState)
          eitherErrorHandler.apply(sqlException) shouldEqual Left(ConnectionError(sqlException))
          shouldBeWithCause(eitherErrorHandler.apply(sqlException), ConnectionError(sqlException), sqlException)
        }
    }

    "convert timeout transient sql error to connection timeout error" in new RedshiftErrorHandling {
      val msg          = "Connection is not available, request timed out after"
      val sqlException = new SQLTransientConnectionException(msg)
      shouldBeWithCause(eitherErrorHandler.apply(sqlException), ConnectionTimeout(msg), sqlException)
    }

    "convert sql error to error with message and state if not timeout" in new RedshiftErrorHandling {
      val msg          = "Other transient error"
      val sqlException = new SQLTransientConnectionException(msg, "not-handled-sql-state", 999)
      shouldBeWithCause(
        eitherErrorHandler.apply(sqlException),
        ErrorWithMessage(s"[not-handled-sql-state] - [999] - $msg"),
        sqlException
      )
    }

  }
}

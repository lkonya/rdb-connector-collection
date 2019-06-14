package com.emarsys.rdb.connector.common.models

import cats.data.EitherT
import cats.instances.future._
import com.emarsys.rdb.connector.common.ConnectorResponseET
import com.emarsys.rdb.connector.common.models.DataManipulation.{Criteria, Record, UpdateDefinition}
import com.emarsys.rdb.connector.common.models.Errors.ConnectorError
import com.emarsys.rdb.connector.common.models.ValidationResult._

import scala.concurrent.{ExecutionContext, Future}

trait RawDataValidator {

  def validateEmptyCriteria(data: Criteria)(
      implicit ec: ExecutionContext
  ): ConnectorResponseET[ValidationResult] =
    EitherT.rightT[Future, ConnectorError] {
      if (data.isEmpty) EmptyData else Valid
    }

  def validateFieldExistence(tableName: String, fields: Set[String], connector: Connector)(
      implicit ec: ExecutionContext
  ): ConnectorResponseET[ValidationResult] = {
    EitherT(connector.listFields(tableName)).map { columns =>
      val nonExistingFields = fields.map(_.toLowerCase).diff(columns.map(_.name.toLowerCase).toSet)
      if (nonExistingFields.isEmpty) {
        Valid
      } else {
        NonExistingFields(fields.filter(field => nonExistingFields.contains(field.toLowerCase)))
      }
    }
  }

  def validateTableExists(tableName: String, connector: Connector)(
      implicit ec: ExecutionContext
  ): ConnectorResponseET[ValidationResult] = {
    validateTableExistsAndIfView(tableName, connector, canBeView = true)
  }

  def validateTableExistsAndNotView(tableName: String, connector: Connector)(
      implicit ec: ExecutionContext
  ): ConnectorResponseET[ValidationResult] = {
    validateTableExistsAndIfView(tableName, connector, canBeView = false)
  }

  private def validateTableExistsAndIfView(tableName: String, connector: Connector, canBeView: Boolean)(
      implicit ec: ExecutionContext
  ): ConnectorResponseET[ValidationResult] = {
    EitherT(connector.listTables()).map { tableModels =>
      tableModels.find(tableModel => tableModel.name == tableName) match {
        case Some(table) => if (!canBeView && table.isView) InvalidOperationOnView else Valid
        case None        => NonExistingTable
      }
    }
  }

  def validateUpdateFields(tableName: String, updateData: Seq[UpdateDefinition], connector: Connector)(
      implicit ec: ExecutionContext
  ): ConnectorResponseET[ValidationResult] = {
    updateData match {
      case Nil => EitherT.rightT[Future, ConnectorError](EmptyData)
      case first :: _ =>
        validateFieldExistence(tableName, first, connector) flatMap {
          case Valid =>
            validateIndices(tableName, first.search.keySet, connector)
          case validationResult => EitherT.rightT[Future, ConnectorError](validationResult)
        }
    }
  }

  private def validateFieldExistence(tableName: String, firstUpdateData: UpdateDefinition, connector: Connector)(
      implicit ec: ExecutionContext
  ): ConnectorResponseET[ValidationResult] = {
    val fields = firstUpdateData.search.keySet ++ firstUpdateData.update.keySet
    validateFieldExistence(tableName, fields, connector)
  }

  def validateIndices(tableName: String, keyFields: Set[String], connector: Connector)(
      implicit ec: ExecutionContext
  ): ConnectorResponseET[ValidationResult] = {
    EitherT(connector.isOptimized(tableName, keyFields.toList))
      .map(isOptimized => if (isOptimized) Valid else NoIndexOnFields)
  }

  def validateFormat(
      data: Seq[Record],
      maxRows: Int
  )(implicit ec: ExecutionContext): ConnectorResponseET[ValidationResult] = {
    val result = data match {
      case Nil => EmptyData
      case first :: _ =>
        if (data.size > maxRows) {
          TooManyRows
        } else if (data.isEmpty) {
          EmptyData
        } else if (!areAllKeysTheSameAs(first, data)) {
          DifferentFields
        } else {
          Valid
        }
    }
    EitherT.rightT[Future, ConnectorError](result)
  }

  private def areAllKeysTheSameAs(compareWith: Record, dataToInsert: Seq[Record]): Boolean = {
    val firstRecordsKeySet = compareWith.keySet
    dataToInsert.forall(_.keySet == firstRecordsKeySet)
  }

  def validateUpdateFormat(
      updateData: Seq[UpdateDefinition],
      maxRows: Int
  )(implicit ec: ExecutionContext): ConnectorResponseET[ValidationResult] =
    EitherT.rightT[Future, ConnectorError] {
      if (updateData.size > maxRows) {
        TooManyRows
      } else if (updateData.isEmpty) {
        EmptyData
      } else if (hasEmptyCriteria(updateData)) {
        EmptyCriteria
      } else if (hasEmptyData(updateData)) {
        EmptyData
      } else if (!areAllCriteriaFieldsTheSame(updateData)) {
        DifferentFields
      } else if (!areAllUpdateFieldsTheSame(updateData)) {
        DifferentFields
      } else {
        Valid
      }
    }

  private def hasEmptyCriteria(updateData: Seq[UpdateDefinition]): Boolean = updateData.exists(_.search.isEmpty)

  private def hasEmptyData(updateData: Seq[UpdateDefinition]): Boolean = updateData.exists(_.update.isEmpty)

  private def areAllCriteriaFieldsTheSame(data: Seq[UpdateDefinition]): Boolean = {
    val firstRecordCriteriaKeySet = data.head.search.keySet
    data.forall(_.search.keySet == firstRecordCriteriaKeySet)
  }

  private def areAllUpdateFieldsTheSame(data: Seq[UpdateDefinition]): Boolean = {
    val firstRecordUpdateKeySet = data.head.update.keySet
    data.forall(_.update.keySet == firstRecordUpdateKeySet)
  }

}

object RawDataValidator extends RawDataValidator
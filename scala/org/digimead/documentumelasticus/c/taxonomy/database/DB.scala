/*
 *
 * This file is part of the Documentum Elasticus project.
 * Copyright (c) 2010-2011 Limited Liability Company «MEZHGALAKTICHESKIJ TORGOVYJ ALIANS»
 * Author: Alexey Aksenov
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Global License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED
 * BY Limited Liability Company «MEZHGALAKTICHESKIJ TORGOVYJ ALIANS»,
 * Limited Liability Company «MEZHGALAKTICHESKIJ TORGOVYJ ALIANS» DISCLAIMS
 * THE WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Global License for more details.
 * You should have received a copy of the GNU Affero General Global License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://www.gnu.org/licenses/agpl.html
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Global License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Global License,
 * you must retain the producer line in every report, form or document
 * that is created or manipulated using Documentum Elasticus.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Documentum Elasticus software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers,
 * serving files in a web or/and network application,
 * shipping Documentum Elasticus with a closed source product.
 *
 * For more information, please contact Documentum Elasticus Team at this
 * address: ezh@ezh.msk.ru
 *
 */

package org.digimead.documentumelasticus.c.taxonomy.database

import com.sun.star.util.DateTime
import java.sql.Connection
import org.digimead.documentumelasticus.database.XDatabase
import java.sql.ResultSet
import org.digimead.documentumelasticus.c.Taxonomy

trait DB {
  val connection: Connection
  def fixDataSource(): Boolean
  def documentCreate(name: String, date: DateTime, description: String): Long
  def documentAddFile(id: Long, file_id: Long, description: String)
  def documentGet(sqlFields: String, sqlConstraint: String, args: Array[Any] = Array()): ResultSet
  //def addFile(file: XFile, interactive: Boolean)
  //def skipFile(file: XFile)
  //def delFile(file: XFile, interactive: Boolean)
}

object DB {
  var db: DB = null
  def initialize(database: XDatabase) {
    if (db != null)
      return
    if (database == null)
      throw new RuntimeException("please, create configuration with appropriate database before load " + Taxonomy.componentName)
    if (database.componentName == "org.digimead.documentumelasticus.database.Office")
      db = new DBOffice(database)
    else
      throw new RuntimeException("this plugin not support " + database.componentName + " component")
    db.fixDataSource()
  }
  def dispose() {
    
  }
}

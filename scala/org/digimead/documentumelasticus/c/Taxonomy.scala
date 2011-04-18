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

package org.digimead.documentumelasticus.c

import com.sun.star.frame.XFrame
import com.sun.star.uno.XComponentContext
import org.digimead.documentumelasticus.Core
import org.digimead.documentumelasticus.Extension
import org.digimead.documentumelasticus.c.taxonomy.ui.wizard.AddNewFile
import org.digimead.documentumelasticus.component.XBase
import org.digimead.documentumelasticus.component.XBaseInfo
import org.digimead.documentumelasticus.database.XDatabase
import org.digimead.documentumelasticus.helper.O
import org.digimead.documentumelasticus.storage.AvailableState
import org.digimead.documentumelasticus.storage.XFile
import org.digimead.documentumelasticus.ui.XUI
import org.digimead.documentumelasticus.ui.TabEntity
import org.digimead.documentumelasticus.ui.TabStoragesOverviewEntity
import org.slf4j.LoggerFactory
import scala.actors.Futures._

class Taxonomy(val ctx: XComponentContext) extends XBase
                                            with TabStoragesOverviewEntity
                                            with TabEntity {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  var componentSingleton = Taxonomy.componentSingleton
  val componentTitle = Taxonomy.componentTitle
  val componentDescription = Taxonomy.componentDescription
  val componentURL = Taxonomy.componentURL
  val componentName = Taxonomy.componentName
  val componentServices = Taxonomy.componentServices
  val componentDisabled = Taxonomy.componentDisabled
  initialize(Array()) // initialized by default
  logger.info(componentName + " active")
  def fileAdd(file: XFile, interactive: Boolean) = if (Taxonomy.initialized)
    Taxonomy.fileAdd(file, interactive)
  else
    throw new RuntimeException("call uninitialized instance of " + componentName)
  def fileSkip(file: XFile, interactive: Boolean) = if (Taxonomy.initialized)
    Taxonomy.fileSkip(file, interactive)
  else
    throw new RuntimeException("call uninitialized instance of " + componentName)
  def fileDel(file: XFile, interactive: Boolean) = if (Taxonomy.initialized)
    Taxonomy.fileDel(file, interactive)
  else
    throw new RuntimeException("call uninitialized instance of " + componentName)
  def tabAdd(container: XFrame, position: Int) = if (Taxonomy.initialized)
    Taxonomy.tabAdd(container, position)
  else
    throw new RuntimeException("call uninitialized instance of " + componentName)
  def isInitialized() = Taxonomy.initialized
  // -----------------------------------
  // - implement trait XInitialization -
  // -----------------------------------
  def initialize(args: Array[AnyRef]) = synchronized {
    logger.info("initialize " + componentName)
    if (isInitialized())
      throw new RuntimeException("Initialization of " + componentName + " already done")
    val extension = ctx.getValueByName("/singletons/org.digimead.documentumelasticus.Extension").asInstanceOf[Extension]
    Taxonomy.initialize(ctx, extension)
    Taxonomy.initialized = true
  }
  // ------------------------------
  // - implement trait XComponent -
  // ------------------------------
  override def dispose():Unit = synchronized {
    logger.info("dispose " + componentName)
    if (!isInitialized()) {
      logger.warn("dispose of " + componentName + " already done")
      return
    }
    Taxonomy.dispose()
    super.dispose()
    Taxonomy.initialized = false
  }
}

object Taxonomy extends XBaseInfo {
  private val logger = LoggerFactory.getLogger(this.getClass.getName)
  var componentSingleton: Option[XBase] = Some(null)
  val componentTitle = "Taxonomy"
  val componentDescription = "Taxonomy plugin"
  val componentURL = "http://www."
  val componentName = classOf[Taxonomy].getName()
  val componentServices: Array[String] = Array(componentName)
  val componentDisabled = false
  var ctx: XComponentContext = null
  var core: Core = null
  var extension: Extension = null
  var wizardAddNewFile: AddNewFile = null
  var initialized: Boolean = false
  logger.info(componentName + " active")
  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    println("Hello, world!")
  }
  /*
   *
   */
  def fileAdd(file: XFile, interactive: Boolean): Boolean = synchronized {
    file.getAvailable(componentName) match {
      case AvailableState.AVAILABLE => {
          logger.debug("skip, file '" + file.getName() + "' already added")
          false
        }
      case AvailableState.BLOCKED => {
          logger.debug("unblock file '" + file.getName() + "'")
          file.setAvailable(componentName, AvailableState.AVAILABLE)
          true
        }
      case AvailableState.NOT_AVAILABLE => {
          logger.debug("add file '" + file.getName() + "'")
          future {
            wizardAddNewFile.execute(file)
          }
//          (new Thread(new Runnable() {def run() {wizard.execute(file)}})).start
          true
        }
    }
  }
  def fileSkip(file: XFile, interactive: Boolean): Boolean = synchronized {
    logger.warn("SKIP FILE")
    true
  }
  def fileDel(file: XFile, interactive: Boolean): Boolean = synchronized {
    logger.warn("DEL FILE")
    true
  }
  def tabAdd(container: XFrame, position: Int) = synchronized {
    val ui = core.getUI().asInstanceOf[XUI]
    taxonomy.ui.Tab.initialize(container, ui, ctx)
    ui.panelAdd(taxonomy.ui.Tab.tab)
  }
  def initialize(arg1: XComponentContext, arg2: Extension) {
    synchronized {
      if (ctx != null)
        return
      ctx = arg1
      extension = arg2
      core = extension.getCore().asInstanceOf[Core]
      taxonomy.database.DB.initialize(core.getDatabase().asInstanceOf[XDatabase])
      wizardAddNewFile = new AddNewFile(ctx, O.SI[XFrame]("com.sun.star.frame.Desktop", ctx))
    }
  }
  def dispose() {
    synchronized {
      if (ctx == null)
        return
      taxonomy.ui.Tab.dispose()
      taxonomy.database.DB.dispose()
      extension = null
      core = null
      ctx = null
      //wizardAddNewFile.dispose() // TODO
      wizardAddNewFile = null
    }
  }
}
//  def panelsAdd(ui: XUI, container: XFrame) = Office.panelsAdd(ui, container)

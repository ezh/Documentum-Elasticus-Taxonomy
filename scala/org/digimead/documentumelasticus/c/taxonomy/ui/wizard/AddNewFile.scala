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

package org.digimead.documentumelasticus.c.taxonomy.ui.wizard

import com.sun.star.awt.ActionEvent
import com.sun.star.awt.XActionListener
import com.sun.star.awt.XButton
import com.sun.star.awt.XControlModel
import com.sun.star.awt.XDateField
import com.sun.star.awt.XDialog
import com.sun.star.awt.XFixedText
import com.sun.star.awt.XTextComponent
import com.sun.star.awt.XToolkit
import com.sun.star.beans.XMultiPropertySet
import com.sun.star.frame.XFrame
import com.sun.star.lang.EventObject
import com.sun.star.uno.XComponentContext
import org.digimead.documentumelasticus.c.taxonomy.database.DB
import org.digimead.documentumelasticus.helper.O
import org.digimead.documentumelasticus.helper.DT
import org.digimead.documentumelasticus.storage.XFile
import org.digimead.documentumelasticus.ui.OControl
import org.digimead.documentumelasticus.ui.MigLayout
import org.digimead.documentumelasticus.ui.OControlContainer
import org.digimead.documentumelasticus.ui.OControlDialog
import org.slf4j.LoggerFactory

class AddNewFile(val ctx: XComponentContext, val parent: XFrame) {
  val logger = LoggerFactory.getLogger(this.getClass)
  private val mcf = ctx.getServiceManager()
  private val toolkit = O.SI[XToolkit](mcf, "com.sun.star.awt.Toolkit", ctx)
  var dialogControl: OControlDialog = null
  var filePage: OControlContainer = null
  var newDocumentPage: OControlContainer = null
  var existsDocumentPage: OControlContainer = null
  var result = 0 // 0 false, 1 new doc, 2 exists doc
  init()
  def init() {
    // create the dialog model and set the properties
    dialogControl = new OControlDialog(mcf, "dialog_container", null, ctx,
                                           new MigLayout("nocache, hidemode 3, insets 0"), arg => {
        arg.control.setModel(O.I[XControlModel](arg.model))
        arg.control.createPeer(toolkit, null)
        arg.setVisible(false)
      })
    addFilePage()
    addNewDocumentPage()
    // existsDocumentPage
    existsDocumentPage = new OControlContainer(mcf, "existsDocumentPage", dialogControl, ctx,
                                           new MigLayout("flowy"))
    addExistsDocumentPage(existsDocumentPage)
    // reset pages
    filePage.setVisible(true)
    newDocumentPage.setVisible(false)
    existsDocumentPage.setVisible(false)
  }
  def execute(file: XFile) {
    // reset size
    dialogControl.getComponents.foreach(control => {
        control.asInstanceOf[OControl].setConstraint("")
      })
    // update
    dialogControl.model.setPropertyValue("Title", "add new file '" + file.getName() + "' to documents")
    updateFilePage(file)
    updateNewDocumentPage(file)
    // recalculate size
    var maximumWidth = 1
    var maximumHeight = 1
    filePage.layout()
    maximumWidth = scala.math.max(filePage.getMinimumWidth(0), maximumWidth)
    maximumHeight = scala.math.max(filePage.getMinimumHeight(0), maximumHeight)
    newDocumentPage.layout()
    maximumWidth = scala.math.max(newDocumentPage.getMinimumWidth(0), maximumWidth)
    maximumHeight = scala.math.max(newDocumentPage.getMinimumHeight(0), maximumHeight)
    existsDocumentPage.layout()
    maximumWidth = scala.math.max(existsDocumentPage.getMinimumWidth(0), maximumWidth)
    maximumHeight = scala.math.max(existsDocumentPage.getMinimumHeight(0), maximumHeight)
    val insets = dialogControl.getInsets()
    maximumWidth = maximumWidth + insets(0) + insets(2)
    maximumHeight = maximumHeight + insets(1) + insets(3)
    //dialogControl.model.setPropertyValue("Width", (maximumWidth * dialogControl.xPixelFactor).toInt)
    //dialogControl.model.setPropertyValue("Height", (maximumHeight * dialogControl.yPixelFactor).toInt)
    dialogControl.setBounds(dialogControl.getX(), dialogControl.getY(), maximumWidth, maximumHeight)
    // resize
    dialogControl.getComponents.foreach(control => {
        control.asInstanceOf[OControl].setConstraint("push, grow, w 100%!, h 100%!")
      })
    // show
    dialogControl.setVisible(true)
    dialogControl.execute()
    dialogControl.setVisible(false)
    // process result
    result match {
      case 2 => ()
      case 1 => {
          // new
          val panelInfo = newDocumentPage.getControl("panelInfo").asInstanceOf[OControlContainer]
          val name = O.I[XTextComponent](panelInfo.getControl("DocumentName").control).getText()
          val date = DT.fromISO(O.I[XDateField](panelInfo.getControl("DocumentDate").control).getDate())
          val description = O.I[XTextComponent](panelInfo.getControl("DocumentDescription").control).getText()
          val docID = DB.db.documentCreate(name, date, description)
          DB.db.documentAddFile(docID, file.getID(), "nnn")
      }
      case 0 => logger.debug("cancel")
    }

    // reset pages
    filePage.setVisible(true)
    newDocumentPage.setVisible(false)
    existsDocumentPage.setVisible(false)
  }
  def addFilePage() {
    var element: OControl = null
    // filePage
    filePage = new OControlContainer(mcf, "filePage", dialogControl, ctx,
                                     new MigLayout("nocache", "[grow, left][shrink]", "[grow]"))
    filePage.setVisible(false)
    val panelInfo = new OControlContainer(mcf, "panelInfo", filePage, ctx,
                                          new MigLayout("nocache"))
    panelInfo.setConstraint("aligny top")
    val panelCtrl = new OControlContainer(mcf, "panelCtrl", filePage, ctx,
                                          new MigLayout("flowy, insets 0", "[right]", "[grow, center][grow, center]"))
    panelCtrl.setConstraint("aligny center")
    // Name, version and permission
    (new OControl(mcf, "NameVersionPermission", "UnoControlFixedText", panelInfo, ctx)).setConstraint("span 4")
    // UUID
    (new OControl(mcf, "UUIDLabel", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 0 1")
    (new OControl(mcf, "UUID", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 1 1, span 3, grow")
    // Service (link)
    (new OControl(mcf, "ServiceLabel", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 0 2")
    (new OControl(mcf, "Service", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 1 2, span 3, grow")
    // Created and Size
    (new OControl(mcf, "CreatedLabel", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 0 3")
    (new OControl(mcf, "Created", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 1 3")
    (new OControl(mcf, "SizeLabel", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 2 3")
    (new OControl(mcf, "Size", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 3 3")
    // Owner and Group
    (new OControl(mcf, "OwnerLabel", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 0 4")
    (new OControl(mcf, "Owner", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 1 4")
    (new OControl(mcf, "GroupLabel", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 2 4")
    (new OControl(mcf, "Group", "UnoControlFixedText", panelInfo, ctx)).setConstraint("cell 3 4")
    // LastUser LastTime
    // Note
    // Description
    val newDocumentButton = new OControl(mcf, "newDocumentButton", "UnoControlButton", panelCtrl, ctx)
    newDocumentButton.setConstraint("w 4cm!, h 4cm!, align 50% 50%")
    O.I[XButton](newDocumentButton.control).setLabel("new")
    O.I[XButton](newDocumentButton.control).addActionListener(new XActionListener() {
        def actionPerformed(e: ActionEvent) {
          logger.warn("hide file")
          filePage.setVisible(false)
          logger.warn("show new doc")
          newDocumentPage.setVisible(true)
          dialogControl.layout()
        }
        def disposing(e: EventObject) {}
      })
    val existsDocumentButton = new OControl(mcf, "existsDocumentButton", "UnoControlButton", panelCtrl, ctx)
    existsDocumentButton.setConstraint("w 4cm!, h 4cm!, align 50% 50%")
    O.I[XButton](existsDocumentButton.control).setLabel("exists")
    O.I[XButton](existsDocumentButton.control).addActionListener(new XActionListener() {
        def actionPerformed(e: ActionEvent) {
          logger.warn("hide file")
          filePage.setVisible(false)
          logger.warn("show exists doc")
          existsDocumentPage.setVisible(true)
          dialogControl.layout()
        }
        def disposing(e: EventObject) {}
      })
  }
  def updateFilePage(file: XFile) {
    val panelInfo = filePage.getControl("panelInfo").asInstanceOf[OControlContainer]
    O.I[XFixedText](panelInfo.getControl("NameVersionPermission").control).setText(file.getName() + " version " + file.getVersion + " permission " + file.getPermission())
    O.I[XFixedText](panelInfo.getControl("UUIDLabel").control).setText("UUID:")
    O.I[XFixedText](panelInfo.getControl("UUID").control).setText(file.getUUID())
    O.I[XFixedText](panelInfo.getControl("ServiceLabel").control).setText("Service:")
    O.I[XFixedText](panelInfo.getControl("Service").control).setText(file.getService())
    O.I[XFixedText](panelInfo.getControl("CreatedLabel").control).setText("Created:")
    O.I[XFixedText](panelInfo.getControl("Created").control).setText(file.getCreatedAt().toString)
    O.I[XFixedText](panelInfo.getControl("SizeLabel").control).setText("Size:")
    O.I[XFixedText](panelInfo.getControl("Size").control).setText(file.getSize().toString)
    O.I[XFixedText](panelInfo.getControl("OwnerLabel").control).setText("Owner:")
    O.I[XFixedText](panelInfo.getControl("Owner").control).setText(file.getOwner().getGivenname())
    O.I[XFixedText](panelInfo.getControl("GroupLabel").control).setText("Group:")
    O.I[XFixedText](panelInfo.getControl("Group").control).setText(file.getGroup().getName())
  }
  def addNewDocumentPage() {
    var element: OControl = null
    newDocumentPage = new OControlContainer(mcf, "newDocumentPage", dialogControl, ctx,
                                           new MigLayout("", "[grow, left]", "[grow, top][shrink, bottom]"))
    filePage.setVisible(false)
    val panelInfo = new OControlContainer(mcf, "panelInfo", newDocumentPage, ctx,
                                          new MigLayout("nocache, hidemode 3, insets 0"))
    panelInfo.setConstraint("cell 0 0, grow")
    val panelCtrl = new OControlContainer(mcf, "debug, panelCtrl, insets 0", newDocumentPage, ctx,
                                          new MigLayout())
    panelCtrl.setConstraint("cell 0 1, alignx right")
    val tempElement = new OControl(mcf, "tempElement", "UnoControlFixedText", panelInfo, ctx)
    tempElement.setVisible(true)
    val tempHeight = tempElement.getPreferredHeight(0)
    (new OControl(mcf, "DocumentName", "UnoControlEdit", panelInfo, ctx, arg => {
          arg.model.setPropertyValue("MultiLine", true)
        })).setConstraint("cell 0 0, w 100%, h " + (tempHeight * 3.1).toInt + "!")
    (new OControl(mcf, "DocumentDate", "UnoControlDateField", panelInfo, ctx, arg => {
          arg.model.setPropertyValue("Dropdown", true)
        })).setConstraint("cell 0 1, w 6cm::12cm")
    (new OControl(mcf, "DocumentDescription", "UnoControlEdit", panelInfo, ctx, arg => {
          arg.model.setPropertyValue("MultiLine", true)
        })).setConstraint("cell 0 2, w 100%, h 100%")
    val newDocumentButton = new OControl(mcf, "newDocumentButton", "UnoControlButton", panelCtrl, ctx)
    O.I[XButton](newDocumentButton.control).setLabel("OK")
    O.I[XButton](newDocumentButton.control).addActionListener(new XActionListener() {
        def actionPerformed(e: ActionEvent) {
          result = 1 // OK, create new document
          O.I[XDialog](dialogControl.control).endExecute()
        }
        def disposing(e: EventObject) {}
      })
    val existsDocumentButton = new OControl(mcf, "existsDocumentButton", "UnoControlButton", panelCtrl, ctx)
    //existsDocumentButton.setConstraint("w 4cm!, h 4cm!, align 50% 50%")
    O.I[XButton](existsDocumentButton.control).setLabel("Cancel")
    O.I[XButton](existsDocumentButton.control).addActionListener(new XActionListener() {
        def actionPerformed(e: ActionEvent) {
          result = 0 // Cancel
          O.I[XDialog](dialogControl.control).endExecute()
        }
        def disposing(e: EventObject) {}
      })
  }
  def updateNewDocumentPage(file: XFile) {
    val panelInfo = newDocumentPage.getControl("panelInfo").asInstanceOf[OControlContainer]
    O.I[XTextComponent](panelInfo.getControl("DocumentName").control).setText(file.getName())
    O.I[XDateField](panelInfo.getControl("DocumentDate").control).setDate(DT.toISO(file.getCreatedAt()))
  }
  def addExistsDocumentPage(parent: OControlContainer) {
    var element: OControl = null
    // Name, version and permission
    element = new OControl(mcf, "NameNNNNN1", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN2", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN3", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN4", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN5", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN6", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN7", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN8", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN9", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN10", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN11", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN12", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN13", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN14", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN15", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN16", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN17", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN18", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN19", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
    element = new OControl(mcf, "NameNNNNN20", "UnoControlFixedText", parent, ctx)
    O.I[XFixedText](element.control).setText("jjj")
  }
}


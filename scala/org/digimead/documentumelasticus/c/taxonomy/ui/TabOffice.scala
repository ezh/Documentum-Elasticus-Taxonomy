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

package org.digimead.documentumelasticus.c.taxonomy.ui

import com.sun.star.awt.XButton
import com.sun.star.awt.XWindow
import com.sun.star.awt.tree.TreeExpansionEvent
import com.sun.star.awt.tree.XMutableTreeDataModel
import com.sun.star.awt.tree.XMutableTreeNode
import com.sun.star.awt.tree.XTreeControl
import com.sun.star.awt.tree.XTreeExpansionListener
import com.sun.star.beans.PropertyValue
import com.sun.star.frame.XComponentLoader
import com.sun.star.frame.XFrame
import com.sun.star.frame.XFramesSupplier
import com.sun.star.lang.EventObject
import com.sun.star.lang.XMultiComponentFactory
import com.sun.star.sheet.XSpreadsheet
import com.sun.star.sheet.XSpreadsheetDocument
import com.sun.star.table.XCell
import com.sun.star.uno.AnyConverter
import com.sun.star.uno.XComponentContext
import org.digimead.documentumelasticus.Core
import org.digimead.documentumelasticus.Extension
import org.digimead.documentumelasticus.component.XBase
import org.digimead.documentumelasticus.helper.O
import org.digimead.documentumelasticus.storage.XFile
import org.digimead.documentumelasticus.storage.XFolderUNO
import org.digimead.documentumelasticus.ui.MigLayout
import org.digimead.documentumelasticus.ui.OControl
import org.digimead.documentumelasticus.ui.OControlContainer
import org.digimead.documentumelasticus.ui.TabStoragesOverview
import org.slf4j.LoggerFactory

class TabOffice(override val name: String,
                override val parentFrame: XFrame,
                override val ctx: XComponentContext) extends Tab(name, parentFrame, ctx) {
  val logger = LoggerFactory.getLogger(this.getClass)
  private val core = ctx.getValueByName("/singletons/org.digimead.documentumelasticus.Core").asInstanceOf[Core]
  private val extension = ctx.getValueByName("/singletons/org.digimead.documentumelasticus.Extension").asInstanceOf[Extension]
  // "private:graphicrepository" means images.zip
  private val treeImgDir = "private:graphicrepository";
  private val treeExpandedURL  = treeImgDir + "/res/folderop.png"
  private val treeCollapsedURL = treeImgDir + "/res/foldercl.png"
  val (frame, container, treeModel, treeRoot, xSpreadsheetDocument) = init()
  def init(): (XFrame, OControlContainer,
               XMutableTreeDataModel, XMutableTreeNode, XSpreadsheetDocument) = {
    val (frame, container) = super.init(new MigLayout("insets 0, nocache")) // TODO fix bug and remove nocache
    val navigationContainer = new OControlContainer(mcf, "tree_container", container, ctx, new MigLayout("nocache, insets 0, gap 0, fill")).setConstraint("w 30%:30%:8cm, h 100%")
    val (treeDataModel,treeRootNode) = addCategoriesTree(navigationContainer)
    val (treeDataModel1,treeRootNode1) = addDateTree(navigationContainer)
    /*
     * buttons
     */
    val button1 = new OControl(mcf, "button_add", "UnoControlButton", navigationContainer, ctx).setConstraint("cell 0 4")
    val button2 = new OControl(mcf, "button_edit", "UnoControlButton", navigationContainer, ctx).setConstraint("cell 0 4")
    val button3 = new OControl(mcf, "button_del", "UnoControlButton", navigationContainer, ctx).setConstraint("cell 0 4")
    val button4 = new OControl(mcf, "button_N", "UnoControlButton", navigationContainer, ctx).setConstraint("cell 0 4")
    val dataComponent = addDataViewer(frame, container)
    // create components grid
    // row 2 - core
    /*TabStoragesOverview.dataRows.append((extension.component(core.componentName),
                                         (mcf: XMultiComponentFactory,
                                          file: XBase,
                                          component: Extension.componentInfo,
                                          cell: XCell,
                                          sheet: XSpreadsheet,
                                          document: XSpreadsheetDocument,
                                          tab: TabStoragesOverview,
                                          ctx: XComponentContext) => {
          if (file.isInstanceOf[XFile])
            TabStoragesOverview.fileSetComponent(mcf, file.asInstanceOf[XFile], component, cell, sheet, xSpreadsheetDocument, tab, ctx)
        }))*/
    (frame, container, treeDataModel,treeRootNode, dataComponent)
  }
  def addCategoriesTree(parent: OControlContainer): (XMutableTreeDataModel, XMutableTreeNode) = {
    val button = new OControl(mcf, "button_categories", "UnoControlButton", parent, ctx).setConstraint("cell 0 0, w 100%!")
    O.I[XButton](button.control).setLabel("asd")
    val tree = new OControl(mcf, "tree_categories", "tree.TreeControl", parent, ctx).setConstraint("cell 0 1, w 100%!, push, grow")
    tree.model.setPropertyValue("SelectionType", com.sun.star.view.SelectionType.SINGLE)
    val treeDataModel = O.SI[XMutableTreeDataModel](mcf, "com.sun.star.awt.tree.MutableTreeDataModel", ctx)
    // tree expand listener
    O.I[XTreeControl](tree.control).addTreeExpansionListener(new TreeListener())
    // add root node
    val treeRootNode = treeDataModel.createNode("documentum elasticus", true)
    // set images of the root
    treeRootNode.setExpandedGraphicURL(treeExpandedURL)
    treeRootNode.setCollapsedGraphicURL(treeCollapsedURL)
    // set data to the root
    treeDataModel.setRoot(treeRootNode)
    tree.model.setPropertyValue("DataModel", treeDataModel)
    (treeDataModel,treeRootNode)
  }
  def addDateTree(parent: OControlContainer): (XMutableTreeDataModel, XMutableTreeNode) = {
    val button = new OControl(mcf, "button_dates", "UnoControlButton", parent, ctx).setConstraint("cell 0 2, w 100%!")
    O.I[XButton](button.control).setLabel("asd")
    val tree = new OControl(mcf, "tree_dates", "tree.TreeControl", parent, ctx).setConstraint("cell 0 3, w 100%!, push, grow")
    tree.model.setPropertyValue("SelectionType", com.sun.star.view.SelectionType.SINGLE)
    val treeDataModel = O.SI[XMutableTreeDataModel](mcf, "com.sun.star.awt.tree.MutableTreeDataModel", ctx)
    // tree expand listener
    O.I[XTreeControl](tree.control).addTreeExpansionListener(new TreeListener())
    // add root node
    val treeRootNode = treeDataModel.createNode("documentum elasticus", true)
    // set images of the root
    treeRootNode.setExpandedGraphicURL(treeExpandedURL)
    treeRootNode.setCollapsedGraphicURL(treeCollapsedURL)
    // set data to the root
    treeDataModel.setRoot(treeRootNode)
    tree.model.setPropertyValue("DataModel", treeDataModel)
    (treeDataModel,treeRootNode)
  }
  def addDataViewer(parentFrame: XFrame, parent: OControlContainer): XSpreadsheetDocument = {
    val dataPanel = new OControlContainer(mcf, "data_panel", parent, ctx, new MigLayout())
    dataPanel.setConstraint("push, grow")
    // add frame to container
    val xFrame = O.SI[XFrame](mcf, "com.sun.star.frame.Frame", ctx)
    xFrame.initialize(O.I[XWindow](dataPanel.control.getPeer))
    xFrame.setCreator(O.I[XFramesSupplier](parentFrame))
    O.I[XFramesSupplier](parentFrame).getFrames().append(xFrame)
    val dataComponent = O.I[XComponentLoader](xFrame).loadComponentFromURL("private:factory/scalc", "_self", 0, Array[PropertyValue]())
    O.I[XSpreadsheetDocument](dataComponent)
  }
  def refresh() {
    
  }
  // -------------
  // - listeners -
  // -------------
  class TreeListener extends XTreeExpansionListener {
    class WorkerThread(val folder: XFolderUNO, val node: XMutableTreeNode, val waitNode: XMutableTreeNode) extends Runnable {
      def run(): Unit = {
        folder.getFiles().sortWith((f1,f2) => f1.getName() < f2.getName()).foreach(file => {
            val storageNode = treeModel.createNode(file.getName(), false)
            storageNode.setDataValue(file.getID())
            node.appendChild(storageNode)
          })
        folder.getFolders().sortWith((f1,f2) => f1.getName() < f2.getName()).foreach(folder => {
            val storageNode = treeModel.createNode(folder.getName(), true)
            storageNode.setExpandedGraphicURL(treeExpandedURL)
            storageNode.setCollapsedGraphicURL(treeCollapsedURL)
            storageNode.setDataValue(folder.getID())
            node.appendChild(storageNode)
          })
        if (node.getChildCount() == 1) {
          val newNode = treeModel.createNode("empty, click to new...", false)
          node.appendChild(newNode)
        }
        val idx = node.getIndex(waitNode)
        if (idx >= 0)
          node.removeChildByIndex(idx)
      }
    }
    def requestChildNodes(e: TreeExpansionEvent) {
      logger.debug("requestChildNodes")
      val node = O.I[XMutableTreeNode](e.Node)
      if (node == null)
        return
      val dataValue = node.getDataValue()
      if (!AnyConverter.isLong(dataValue))
        return
      val id = AnyConverter.toLong(dataValue)
      while (node.getChildCount() > 0)
        node.removeChildByIndex(0)
      val waitNode = treeModel.createNode("Please wait...", false)
      node.appendChild(waitNode)
      if (node.getParent() == treeRoot) {
        // storage
        logger.debug("process storage id " + id)
        try {
          val worker = new WorkerThread(core.getStorageByID(id).getRoot(), node, waitNode)
          val thread = new Thread(worker)
          thread.setDaemon(true)
          thread.start()
        } catch {
          case e => {
              logger.error(e.getMessage(), e)
              val idx = node.getIndex(waitNode)
              if (idx >= 0)
                node.removeChildByIndex(idx)
              val errNode = treeModel.createNode("Error. " + e.getMessage, false)
              node.appendChild(errNode)
          }
        }
      } else {
        // regular node
        logger.debug("process node id " + id)
        try {
          val worker = new WorkerThread(core.getFolderByID(id), node, waitNode)
          val thread = new Thread(worker)
          thread.setDaemon(true)
          thread.start()
        } catch {
          case e => {
              logger.error(e.getMessage(), e)
              val idx = node.getIndex(waitNode)
              if (idx >= 0)
                node.removeChildByIndex(idx)
              val errNode = treeModel.createNode("Error. " + e.getMessage, false)
              node.appendChild(errNode)
          }
        }
      }
    }
    def treeExpanding(e: TreeExpansionEvent) {
      logger.debug("treeExpanding")
    }
    def treeCollapsing(e: TreeExpansionEvent) {
      logger.debug("treeCollapsing")
    }
    def treeExpanded(e: TreeExpansionEvent) {
      logger.debug("treeExpanded")
    }
    def treeCollapsed(e: TreeExpansionEvent) {
      logger.debug("treeCollapsed")
    }
    // ----------------------------------
    // - implement trait XEventListener -
    // ----------------------------------
    def disposing(e: EventObject) = {
      logger.trace("disposing")
    }
  }
}
/*
 * DETab(name,
                                                                   parentFrame,
                                                                   ctx) with 
 */
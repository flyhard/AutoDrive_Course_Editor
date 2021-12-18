package AutoDriveEditor;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.*;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import AutoDriveEditor.MapHelpers.CopyPasteManager;
import static AutoDriveEditor.ADUtils.*;
import static AutoDriveEditor.AutoDriveEditor.*;
import static AutoDriveEditor.MapPanel.*;
import static AutoDriveEditor.GUIBuilder.*;


public class EditorListener implements ActionListener, ItemListener, ChangeListener, MouseListener {

    public AutoDriveEditor editor;

    public EditorListener (AutoDriveEditor editor) {
        this.editor = editor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        LOG.info("ActionCommand: {}", e.getActionCommand());

        JFileChooser fc = new JFileChooser();
        MapPanel.getMapPanel().isMultiSelectAllowed = false;

        switch (e.getActionCommand()) {
            case MENU_LOAD_CONFIG:
                if (MapPanel.getMapPanel().isStale()) {
                    int response = JOptionPane.showConfirmDialog(editor, localeString.getString("dialog_exit_unsaved"), "AutoDrive", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (response == JOptionPane.YES_OPTION) {
                        editor.saveMap(null);
                    }
                }
                fc.setDialogTitle(localeString.getString("dialog_load_config_title"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                FileNameExtensionFilter filter = new FileNameExtensionFilter("AutoDrive config", "xml");
                fc.addChoosableFileFilter(filter);

                if (fc.showOpenDialog(editor) == JFileChooser.APPROVE_OPTION) {
                    MapPanel.getMapPanel().confirmCurve();
                    File fileName = fc.getSelectedFile();
                    editor.loadConfigFile(fileName);
                    forceMapImageRedraw();
                    isUsingConvertedImage = false;
                    GUIBuilder.saveImageEnabled(false);
                }
                break;
            case MENU_SAVE_CONFIG:
                editor.saveMap(null);
                break;
            case MENU_SAVE_SAVEAS:
                if (editor.xmlConfigFile == null) break;
                fc.setDialogTitle(localeString.getString("dialog_save_destination"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                FileNameExtensionFilter savefilter = new FileNameExtensionFilter("AutoDrive config", "xml");
                fc.setSelectedFile(editor.xmlConfigFile);
                fc.addChoosableFileFilter(savefilter);

                if (fc.showSaveDialog(editor) == JFileChooser.APPROVE_OPTION) {
                    LOG.info("{} {}", localeString.getString("console_config_saveas"), ADUtils.getSelectedFileWithExtension(fc));
                    editor.saveMap(ADUtils.getSelectedFileWithExtension(fc).toString());
                }
                break;
            case MENU_EXIT:
                editor.dispatchEvent(new WindowEvent(editor, WindowEvent.WINDOW_CLOSING));
                break;
            case MENU_EDIT_CUT:
                break;
            case MENU_EDIT_COPY:
                break;
            case MENU_EDIT_PASTE:
                break;
            case MENU_LOAD_IMAGE:
                fc.setDialogTitle(localeString.getString("dialog_load_image_title"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                if (fc.showOpenDialog(editor) == JFileChooser.APPROVE_OPTION) {
                    File fileName;
                    try {
                        fileName = fc.getSelectedFile();
                        BufferedImage mapImage = ImageIO.read(fileName);
                        if (mapImage != null) {
                            MapPanel.getMapPanel().setImage(mapImage);
                            forceMapImageRedraw();
                            //MapPanel.getMapPanel().moveMapBy(0,1); // hacky way to get map image to refresh
                        }
                    } catch (IOException e1) {
                        LOG.error(e1.getMessage(), e1);
                    }
                }
                break;
            case MENU_SAVE_IMAGE:
                String currentPath;
                if (!oldConfigFormat) {
                    currentPath = ADUtils.getCurrentLocation() + "mapImages/" + mapName + ".png";
                } else {
                    currentPath = ADUtils.getCurrentLocation() + "mapImages/unknown.png";
                }

                LOG.info("currentpath = {}", currentPath);
                File path = new File(currentPath);
                try {
                    if (path.exists()) {
                        if (path.isDirectory())
                            throw new IOException("File '" + path + "' is a directory");

                        if (!path.canWrite())
                            throw new IOException("File '" + path + "' cannot be written");
                    } else {
                        File parent = path.getParentFile();
                        LOG.info("parent = {}", parent.getName());
                        if (!parent.exists() && (!parent.mkdirs())) {
                            throw new IOException("'" + path + "' could not be created");
                        }
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

                LOG.info("path = {}", currentPath);
                fc.setDialogTitle(localeString.getString("dialog_save_mapimage"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("AutoDrive Map Image", "png");
                fc.setSelectedFile(path);
                fc.setCurrentDirectory(path);
                fc.addChoosableFileFilter(imageFilter);

                if (fc.showSaveDialog(editor) == JFileChooser.APPROVE_OPTION) {
                    File saveImageFile = ADUtils.getSelectedFileWithExtension(fc);
                    if (saveImageFile.exists()) {
                        int response = JOptionPane.showConfirmDialog(editor, localeString.getString("dialog_mapimage_overwrite"), "File already exists " + mapName + ".png", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (response == JOptionPane.NO_OPTION) {
                            LOG.info("Cancelled saving of converted image");
                            break;
                        }
                    }
                    LOG.info("{} {}", localeString.getString("console_map_saveimage"), saveImageFile);
                    saveMapImage(ADUtils.getSelectedFileWithExtension(fc).toString());
                }
                break;
            case MENU_IMPORT_FS19_DDS:
                fc.setDialogTitle(localeString.getString("dialog_import_FS19_dds_image_title"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        // always accept directory's
                        if ( f.isDirectory() ) return true;
                        // but only files with a s pecific name
                        return f.getName().equals("pda_map_H.dds");
                    }

                    @Override
                    public String getDescription() {
                        return ".dds";
                    }
                });
                if (fc.showOpenDialog(editor) == JFileChooser.APPROVE_OPTION) {
                    if ( !fc.getSelectedFile().getName().equals("pda_map_H.dds") && !fc.getSelectedFile().getName().endsWith(".dds")) {
                        JOptionPane.showMessageDialog(editor, "The file " + fc.getSelectedFile() + " is not a valid dds file.", "FileType Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    }

                    LOG.info("Valid Filename {}", fc.getSelectedFile().getAbsoluteFile());
                    boolean result = importFromFS19(fc.getSelectedFile().getAbsoluteFile().toString());
                    if (result) {
                        isUsingConvertedImage = true;
                        GUIBuilder.saveImageEnabled(true);
                    }
                } else {
                    LOG.info("Cancelled FS19 PDA Image Import");
                }
                break;
            case MENU_IMPORT_FS22_DDS:
                fc.setDialogTitle(localeString.getString("dialog_import_FS22_dds_image_title"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        // always accept directory's
                        if ( f.isDirectory() ) return true;
                        // but only files with a s pecific name
                        return f.getName().equals("overview.dds");
                    }

                    @Override
                    public String getDescription() {
                        return ".dds";
                    }
                });
                if (fc.showOpenDialog(editor) == JFileChooser.APPROVE_OPTION) {
                    if ( !fc.getSelectedFile().getName().equals("overview.dds") && !fc.getSelectedFile().getName().endsWith(".dds")) {
                        JOptionPane.showMessageDialog(editor, "The file " + fc.getSelectedFile() + " is not a valid dds file.", "FileType Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    }

                    LOG.info("Valid Filename {}", fc.getSelectedFile().getAbsoluteFile());
                    boolean result = importFromFS22(fc.getSelectedFile().getAbsoluteFile().toString());
                    if (result) {
                        isUsingConvertedImage = true;
                        GUIBuilder.saveImageEnabled(true);
                    }
                } else {
                    LOG.info("Cancelled FS22 PDA Image Import");
                }
                break;
            case MENU_ZOOM_1x:
                editor.updateMapZoomFactor(1);
                break;
            case MENU_ZOOM_4x:
                editor.updateMapZoomFactor(2);
                break;
            case MENU_ZOOM_16x:
                editor.updateMapZoomFactor(4);
                break;
            case MENU_ABOUT:
                showAbout();
                break;
            case BUTTON_MOVE_NODES:
                editorState = EDITORSTATE_MOVING;
                MapPanel.getMapPanel().isMultiSelectAllowed = true;
                break;
            case BUTTON_CONNECT_NODES:
                editorState = EDITORSTATE_CONNECTING;
                connectionType=CONNECTION_STANDARD;
                break;
            case BUTTON_CREATE_PRIMARY_NODE:
                editorState = EDITORSTATE_CREATE_PRIMARY_NODE;
                break;
            case BUTTON_CREATE_DUAL_CONNECTION:
                editorState = EDITORSTATE_CONNECTING;
                connectionType=CONNECTION_DUAL;
                break;
            case BUTTON_CHANGE_NODE_PRIORITY:
                editorState = EDITORSTATE_CHANGE_NODE_PRIORITY;
                MapPanel.getMapPanel().isMultiSelectAllowed = true;
                break;
            case BUTTON_CREATE_SUBPRIO_NODE:
                editorState = EDITORSTATE_CREATE_SUBPRIO_NODE;
                break;
            case BUTTON_CREATE_REVERSE_CONNECTION:
                editorState = EDITORSTATE_CONNECTING;
                connectionType=CONNECTION_REVERSE;
                break;
            case BUTTON_REMOVE_NODES:
                editorState = EDITORSTATE_DELETE_NODES;
                MapPanel.getMapPanel().isMultiSelectAllowed = true;
                break;
            case BUTTON_CREATE_DESTINATIONS:
                editorState = EDITORSTATE_CREATING_DESTINATION;
                break;
            case BUTTON_EDIT_DESTINATIONS_GROUPS:
                editorState = EDITORSTATE_EDITING_DESTINATION;
                break;
            case BUTTON_DELETE_DESTINATIONS:
                editorState = EDITORSTATE_DELETING_DESTINATION;
                MapPanel.getMapPanel().isMultiSelectAllowed = true;
                break;
            case BUTTON_ALIGN_HORIZONTAL:
                editorState = EDITORSTATE_ALIGN_HORIZONTAL;
                MapPanel.getMapPanel().isMultiSelectAllowed = true;
                break;
            case BUTTON_ALIGN_VERTICAL:
                editorState = EDITORSTATE_ALIGN_VERTICAL;
                MapPanel.getMapPanel().isMultiSelectAllowed = true;
                break;
            case BUTTON_CREATE_QUADRATICBEZIER:
                editorState = EDITORSTATE_QUADRATICBEZIER;
                break;
            case BUTTON_CREATE_CUBICBEZIER:
                editorState = EDITORSTATE_CUBICBEZIER;
                break;
            case BUTTON_COMMIT_CURVE:
                MapPanel.getMapPanel().confirmCurve();
                break;
            case BUTTON_CANCEL_CURVE:
                MapPanel.getMapPanel().cancelCurve();
                break;
            case BUTTON_COPYPASTE_SELECT:
                editorState = EDITORSTATE_CNP_SELECT;
                MapPanel.getMapPanel().isMultiSelectAllowed = true;
                break;
            case BUTTON_COPYPASTE_CUT:
                cutSelected();
                break;
            case BUTTON_COPYPASTE_COPY:
                copySelected();
                break;
            case BUTTON_COPYPASTE_PASTE:
                pasteSelected();
                break;
            case MENU_EDIT_UNDO:
                changeManager.undo();
                enableMultiSelect();
                break;
            case MENU_EDIT_REDO:
                changeManager.redo();
                enableMultiSelect();
                break;
            case MENU_GRID_SET:
                MapPanel.getMapPanel().showGridSettingDialog();
                break;
            case MENU_ROTATE_SET:
                MapPanel.getMapPanel().showRotationSettingDialog();
                break;
            case MENU_ROTATE_CLOCKWISE:
                CopyPasteManager.rotateSelected(rotationAngle);
                break;
            case MENU_ROTATE_ANTICLOCKWISE:
                CopyPasteManager.rotateSelected(-rotationAngle);
                break;
            case MENU_ROTATE_CLOCKWISE_NINTY:
                CopyPasteManager.rotateSelected(90);
                break;
            case MENU_ROTATE_ANTICLOCKWISE_NINTY:
                CopyPasteManager.rotateSelected(-90);
                break;
        }
        updateButtons();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        AbstractButton button = (AbstractButton) e.getItem();
        switch (button.getActionCommand()) {
            case MENU_CHECKBOX_CONTINUECONNECT:
                AutoDriveEditor.bContinuousConnections = button.isSelected();
                break;
            case MENU_CHECKBOX_MIDDLEMOUSEMOVE:
                AutoDriveEditor.bMiddleMouseMove = button.isSelected();
                break;
            case MENU_GRID_SHOW:
                GUIBuilder.bShowGrid = button.isSelected();
                MapPanel.getMapPanel().repaint();
                break;
            case MENU_GRID_SNAP:
                GUIBuilder.bGridSnap = button.isSelected();
                if (!button.isSelected()) {
                    bGridSnapSubs = false;
                    gridSnapSubDivisionMenuItem.setSelected(false);
                }
                break;
            case MENU_GRID_SNAP_SUBS:
                GUIBuilder.bGridSnapSubs = button.isSelected();
                break;
            case MENU_DEBUG_SHOWID:
                GUIBuilder.bDebugShowID = button.isSelected();
                mapPanel.repaint();
                break;
            case MENU_DEBUG_SELECTED_LOCATION:
                GUIBuilder.bDebugShowSelectedLocation = button.isSelected();
                break;
            case MENU_DEBUG_FILEIO:
                GUIBuilder.bDebugFileIO = button.isSelected();
                break;
            case MENU_DEBUG_PROFILE:
                GUIBuilder.bDebugProfile = button.isSelected();
                break;
            case MENU_DEBUG_UNDO:
                GUIBuilder.bDebugUndoRedo = button.isSelected();
                break;
            case MENU_DEBUG_TEST:
                GUIBuilder.bDebugTest = button.isSelected();
                isDraggingNode = true;
                MapPanel.getMapPanel().mouseDragged(-1, 0);
                break;
            case RADIOBUTTON_PATHTYPE_REGULAR:
                if (quadCurve != null && isQuadCurveCreated) {
                    quadCurve.setNodeType(NODE_STANDARD);
                } else if (cubicCurve != null && isCubicCurveCreated) {
                    cubicCurve.setNodeType(NODE_STANDARD);
                }
                mapPanel.repaint();
                break;
            case RADIOBUTTON_PATHTYPE_SUBPRIO:
                if (quadCurve != null && isQuadCurveCreated) {
                    quadCurve.setNodeType(NODE_SUBPRIO);
                } else if (cubicCurve != null && isCubicCurveCreated) {
                    cubicCurve.setNodeType(NODE_SUBPRIO);
                }
                mapPanel.repaint();
                break;
            case RADIOBUTTON_PATHTYPE_REVERSE:
                if (button.isSelected()) {
                    GUIBuilder.curvePathDual.setSelected(false);
                }
                if (quadCurve != null) {
                    quadCurve.setReversePath(button.isSelected());
                    quadCurve.setDualPath(false);
                } else if (cubicCurve != null) {
                    cubicCurve.setReversePath(button.isSelected());
                    cubicCurve.setDualPath(false);
                }
                mapPanel.repaint();
                break;
            case RADIOBUTTON_PATHTYPE_DUAL:
                if (button.isSelected()) {
                    GUIBuilder.curvePathReverse.setSelected(false);

                }
                if (quadCurve != null) {
                    quadCurve.setDualPath(button.isSelected());
                    quadCurve.setReversePath(false);
                } else if (cubicCurve != null) {
                    cubicCurve.setDualPath(button.isSelected());
                    cubicCurve.setReversePath(false);
                }

                mapPanel.repaint();
                break;
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(editor, "<html><center>Editor version : " + AUTODRIVE_INTERNAL_VERSION + "<br>Build info : Java 11 SDK - IntelliJ IDEA 2021.2.3 Community Edition<br><br><u>AutoDrive Development Team</u><br><br><b>Stephan (Founder & Modder)</b><br><br>TyKonKet (Modder)<br>Oliver (Modder)<br>Axel (Co-Modder)<br>Aletheist (Co-Modder)<br>Willi (Supporter & Tester)<br>Iwan1803 (Community Manager & Supporter)", "AutoDrive Editor", JOptionPane.PLAIN_MESSAGE);
    }


    @Override
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if (source.getValueIsAdjusting()) {
            int value = source.getValue();
            if (value < 2) value = 2;
            if (MapPanel.quadCurve != null) {
                MapPanel.quadCurve.setNumInterpolationPoints(value);
                MapPanel.getMapPanel().repaint();
            } else if (cubicCurve != null) {
                MapPanel.cubicCurve.setNumInterpolationPoints(value);
                MapPanel.getMapPanel().repaint();
            }
        }
    }

    // These mouse events are only triggered by StateChangeJToggleButtons
    // with a right click on button.
    // all other mouse functions are still handled by the MouseListener

    @Override
    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1) {
            JToggleButton toggleStateButton = (JToggleButton) e.getSource();
            if (toggleStateButton.isEnabled()) {
                //toggleStateButton.setEnabled(false);
                if (toggleStateButton == GUIBuilder.createRegularConnection) {
                    createRegularConnectionState = 1 - createRegularConnectionState;
                    if (createRegularConnectionState == NODE_STANDARD) { // == 0
                        createRegularConnection.setIcon(regularConnectionIcon);
                        createRegularConnection.setSelectedIcon(regularConnectionSelectedIcon);
                    } else {
                        createRegularConnection.setIcon(regularConnectionSubPrioIcon);
                        createRegularConnection.setSelectedIcon(regularConnectionSubPrioSelectedIcon);
                    }
                } else if (toggleStateButton == createDualConnection) {
                    createDualConnectionState = 1 - createDualConnectionState;
                    if (createDualConnectionState == NODE_STANDARD) { // == 0
                        createDualConnection.setIcon(dualConnectionIcon);
                        createDualConnection.setSelectedIcon(dualConnectionSelectedIcon);
                    } else {
                        createDualConnection.setIcon(dualConnectionSubPrioIcon);
                        createDualConnection.setSelectedIcon(dualConnectionSubPrioSelectedIcon);
                    }
                } else if (toggleStateButton == createReverseConnection) {
                    createReverseConnectionState = 1 - createReverseConnectionState;
                    if (createReverseConnectionState == NODE_STANDARD) { // == 0
                        createReverseConnection.setIcon(reverseConnectionIcon);
                        createReverseConnection.setSelectedIcon(reverseConnectionSelectedIcon);
                    } else {
                        createReverseConnection.setIcon(reverseConnectionSubPrioIcon);
                        createReverseConnection.setSelectedIcon(reverseConnectionSubPrioSelectedIcon);
                    }
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    public static void enableMultiSelect() {
        switch (editorState) {
            case EDITORSTATE_MOVING:
            case EDITORSTATE_CHANGE_NODE_PRIORITY:
            case EDITORSTATE_DELETE_NODES:
            case EDITORSTATE_DELETING_DESTINATION:
            case EDITORSTATE_ALIGN_HORIZONTAL:
            case EDITORSTATE_ALIGN_VERTICAL:
            case EDITORSTATE_CNP_SELECT:
                mapPanel.isMultiSelectAllowed = true;
                return;
        }
        mapPanel.isMultiSelectAllowed = false;
    }
}


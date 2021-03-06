/*
 * FXFlowNodeSkin.java
 * 
 * Copyright 2012-2013 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * Please cite the following publication(s):
 *
 * M. Hoffer, C.Poliwoda, G.Wittum. Visual Reflection Library -
 * A Framework for Declarative GUI Programming on the Java Platform.
 * Computing and Visualization in Science, 2011, in press.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Michael Hoffer <info@michaelhoffer.de>.
 */
package eu.mihosoft.vrl.workflow.fx;

import eu.mihosoft.vrl.workflow.Connection;
import eu.mihosoft.vrl.workflow.Connector;
import eu.mihosoft.vrl.workflow.VFlow;
import eu.mihosoft.vrl.workflow.VNode;
import eu.mihosoft.vrl.workflow.VisualizationRequest;
import eu.mihosoft.vrl.workflow.skin.VNodeSkin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import jfxtras.labs.scene.control.window.Window;
import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.SubLine;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class FXFlowNodeSkin
        implements FXSkin<VNode, Window>, VNodeSkin<VNode> {

    private final ObjectProperty<VNode> modelProperty = new SimpleObjectProperty<>();
    private FlowNodeWindow node;
    private final ObjectProperty<Parent> parentProperty = new SimpleObjectProperty<>();
    private ChangeListener<String> modelTitleListener;
    private ChangeListener<Number> modelXListener;
    private ChangeListener<Number> modelYListener;
    private ChangeListener<Number> modelWidthListener;
    private ChangeListener<Number> modelHeightListener;
    private ChangeListener<Number> nodeXListener;
    private ChangeListener<Number> nodeYListener;
    private ChangeListener<Number> nodeWidthListener;
    private ChangeListener<Number> nodeHeightListener;
//    private Node output;
    private FXNewConnectionSkin newConnectionSkin;
    private MouseEvent newConnectionPressEvent;
    private boolean removeSkinOnly = false;
    VFlow controller;
    Map<Connector, Shape> connectors = new HashMap<>();
    List<List<Shape>> shapeLists = new ArrayList<>();
    private final Map<Connector, Integer> connectorToIndexMap = new HashMap<>();
    private final FXSkinFactory skinFactory;
    private final List<Double> connectorSizes = new ArrayList<>();
//    private double inputConnectorSize;
//    private double outputConnectorSize;
    private final List<Connector> connectorList = new ArrayList<>();
    private final IntegerProperty numConnectorsProperty = new SimpleIntegerProperty();

    private MapChangeListener<String, Object> vReqLister;

    private final int TOP = 0;
    private final int RIGHT = 1;
    private final int BOTTOM = 2;
    private final int LEFT = 3;

    public FXFlowNodeSkin(FXSkinFactory skinFactory, Parent parent, VNode model, VFlow controller) {
        this.skinFactory = skinFactory;
        setParent(parent);
        setModel(model);

        this.controller = controller;

        init();
    }

    private void init() {

        for (int i = 0; i < 4; i++) {
            shapeLists.add(new ArrayList<>());
        }

        node = new FlowNodeWindow(this);

        node.setTitle(getModel().getTitle());
        node.setLayoutX(getModel().getX());
        node.setLayoutY(getModel().getY());
        node.setPrefWidth(getModel().getWidth());
        node.setPrefHeight(getModel().getHeight());

        registerListeners(getModel());

        modelProperty.addListener(new ChangeListener<VNode>() {
            @Override
            public void changed(ObservableValue<? extends VNode> ov, VNode oldVal, VNode newVal) {

                removeListeners(oldVal);
                registerListeners(newVal);
            }
        });

        for (Connector connector : getModel().getConnectors()) {
            addConnector(connector);
        }

        getModel().getConnectors().addListener(new ListChangeListener<Connector>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Connector> change) {
                boolean numConnectorsHasChanged = false;
                while (change.next()) {
                    if (change.wasPermutated()) {
                        for (int i = change.getFrom(); i < change.getTo(); ++i) {
                            //permutate
                        }
                    } else if (change.wasUpdated()) {
                        //update item
                    } else if (change.wasRemoved()) {
                        numConnectorsHasChanged = true;
                        // removed
                        for (Connector connector : change.getRemoved()) {
                            removeConnector(connector);
                        }
                    } else if (change.wasAdded()) {
                        numConnectorsHasChanged = true;
                        // added
                        for (Connector connector : change.getAddedSubList()) {
                            addConnector(connector);
                        }
                    }

                } // end while change.next()

                if (numConnectorsHasChanged) {

                    computeConnectorSizes();
//                    computeInputConnectorSize();
//                    computeOutputConnectorSize();
                    adjustConnectorSize();

                    numConnectorsProperty.set(getModel().getConnectors().size());
                }
                
                configureEditCapability();
            }
        });

        node.boundsInParentProperty().addListener((ov, oldValue, newValue) -> {
            for (Connector c : connectorList) {
                layoutConnector(c, true);
            }
        });
    }

    private void initVReqListeners() {

        configureEditCapability();

        vReqLister = (MapChangeListener.Change<? extends String, ? extends Object> change) -> {
            
            configureEditCapability();
        };

        getModel().getVisualizationRequest().addListener(vReqLister);
    }

    private void configureEditCapability() {
        Optional<Boolean> disableEditing
                = getModel().getVisualizationRequest().
                get(VisualizationRequest.KEY_DISABLE_EDITING);

        if (disableEditing.isPresent()) {

            boolean disableEditingV = disableEditing.get();

            node.setMovable(!disableEditingV);
            node.setResizableWindow(!disableEditingV);

            for (Shape connectorShape : connectors.values()) {
                connectorShape.setMouseTransparent(disableEditingV);
            }
        }

    }

    void layoutConnectors() {
        for (Connector c : connectorList) {
            
            layoutConnector(c, false);
        }
    }

    private void layoutConnector(Connector c, boolean updateOthers) {

        Optional<Boolean> autoLayout
                = c.getVisualizationRequest().
                get(VisualizationRequest.KEY_CONNECTOR_AUTO_LAYOUT);

        boolean switchEdges = false;

        if (autoLayout.isPresent()) {
            switchEdges = autoLayout.get();
        }

        Circle connectorShape = (Circle) connectors.get(c);

        connectorShape.setLayoutX(computeConnectorXValue(c));
        connectorShape.setLayoutY(computeConnectorYValue(c));

        Collection<Connection> conns = getModel().getFlow().
                getConnections(c.getType()).getAllWith(c);

        if (conns.isEmpty()) {
            return;
        }

        Pair<Integer, Integer> edges = new Pair<>(LEFT, RIGHT);

        if (switchEdges) {
            List<Pair<Integer, Integer>> edgesList
                    = new ArrayList<>(conns.size());

            for (Connection connection : conns) {
                Pair<Integer, Integer> tmpEdges
                        = getConnectorEdges(connection);
                edgesList.add(tmpEdges);
            }

            List<Integer> frequencies = edgesList.stream().distinct().
                    map(e -> Collections.frequency(edgesList, e)).
                    collect(Collectors.toList());

            int max = 0;
            int maxIndex = -1;

            for (int i = 0; i < frequencies.size(); i++) {
                int freq = frequencies.get(i);
                if (freq > max) {
                    max = freq;
                    maxIndex = i;
                }
            }

            if (maxIndex > -1) {
                edges = edgesList.get(maxIndex);
            }

            int oldEdgeIndex = connectorToIndexMap.get(c);
            int newEdgeIndex;

            if (c.isInput()) {
                newEdgeIndex = edges.getSecond();

                if (newEdgeIndex == RIGHT) {
                    newEdgeIndex = TOP;
                } else if (newEdgeIndex == BOTTOM) {
                    newEdgeIndex = LEFT;
                }

            } else {
                newEdgeIndex = edges.getFirst();

                if (newEdgeIndex == TOP) {
                    newEdgeIndex = RIGHT;
                } else if (newEdgeIndex == LEFT) {
                    newEdgeIndex = BOTTOM;
                }
            }

            if (newEdgeIndex != oldEdgeIndex) {

                shapeLists.get(oldEdgeIndex).remove(connectorShape);

                shapeLists.get(newEdgeIndex).add(connectorShape);
                connectorToIndexMap.put(c, newEdgeIndex);

                // update all other connectors
                layoutConnectors();
            }
        } // end if switchEdges

        connectorShape.setLayoutX(computeConnectorXValue(c));
        connectorShape.setLayoutY(computeConnectorYValue(c));
        
//        System.out.println("c: " + c);

        if (updateOthers) {
            for (Connection connection : conns) {

                String nId;
                Connector cTmp;

                if (c.isInput()) {
                    nId = connection.getSender().getNode().getId();
                    cTmp = connection.getSender();
                } else {
                    nId = connection.getReceiver().getNode().getId();
                    cTmp = connection.getReceiver();
                }

                List<VNodeSkin> skins = controller.getNodeSkinLookup().
                        getById(nId);

                for (VNodeSkin skin : skins) {
                    FXFlowNodeSkin fxSkin = (FXFlowNodeSkin) skin;
                    fxSkin.layoutConnector(cTmp, false);
                }
            }
        }
    }

    private double computeConnectorXValue(Connector connector) {

        Shape connectorNode = connectors.get(connector);

        double posX = node.getLayoutX();

        int edgeIndex = connector.isInput() ? LEFT : RIGHT;

        if (connectorToIndexMap.containsKey(connector)) {
            edgeIndex = connectorToIndexMap.get(connector);
        }

        if (edgeIndex == RIGHT) {
            posX += node.getWidth();
        }

        if (edgeIndex == LEFT || edgeIndex == RIGHT) {
            return posX;
        }

        double midPointOfNode = posX + node.getWidth() * 0.5;

        double connectorWidth = connectorNode.getBoundsInLocal().getWidth();
        double gap = 5;

        double numConnectors = shapeLists.get(edgeIndex).size();

        int connectorIndex = shapeLists.get(edgeIndex).indexOf(connectorNode);

        double totalWidth = numConnectors * connectorWidth
                + (numConnectors - 1) * gap;

        double startX = midPointOfNode - totalWidth / 2;

        double offsetX = +(connectorWidth + gap) * connectorIndex
                + (connectorWidth + gap) / 2;;

        double x = startX + offsetX;

        return x;
    }

    private double computeConnectorYValue(Connector connector) {

        Shape connectorNode = connectors.get(connector);

        double posY = node.getLayoutY();

        int edgeIndex = connector.isInput() ? LEFT : RIGHT;

        if (connectorToIndexMap.containsKey(connector)) {
            edgeIndex = connectorToIndexMap.get(connector);
        }

        if (edgeIndex == BOTTOM) {
            posY += node.getHeight();
        }

        if (edgeIndex == TOP || edgeIndex == BOTTOM) {
            return posY;
        }

        double connectorHeight = connectorNode.getBoundsInLocal().getHeight();
        double gap = 5;

        double numConnectors = shapeLists.get(edgeIndex).size();

        int connectorIndex = shapeLists.get(edgeIndex).indexOf(connectorNode);

        double totalHeight = numConnectors * connectorHeight
                + (numConnectors - 1) * gap;

        double midPointOfNode = node.getLayoutY()
                + node.getHeight() / 2;

        double startY = midPointOfNode - totalHeight / 2;

        double y = startY;

        double offsetY = +(connectorHeight + gap) * connectorIndex
                + (connectorHeight + gap) / 2;;

        y += offsetY;

        return y;
    }

    private void addConnector(final Connector connector) {

        connectorList.add(connector);
        ConnectorCircle circle = new ConnectorCircle(controller,
                getSkinFactory(), connector, 20);

        final Circle connectorNode = circle;
        connectorNode.setManaged(false);

        connectors.put(connector, connectorNode);

        if (connector.isInput()) {
//            inputList.add(connectorNode);
            shapeLists.get(LEFT).add(connectorNode);
            connectorToIndexMap.put(connector, LEFT);
        } else if (connector.isOutput()) {
//            outputList.add(connectorNode);
            shapeLists.get(RIGHT).add(connectorNode);
            connectorToIndexMap.put(connector, RIGHT);
        }

        node.boundsInLocalProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable,
                    Bounds oldValue, Bounds newValue) {

                computeConnectorSizes();
                adjustConnectorSize();
            }
        });

        NodeUtil.addToParent(getParent(), connectorNode);

        connectorNode.onMouseEnteredProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                connectorNode.toFront();
            }
        });

        connectorNode.onMousePressedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {

                // we are already connected and manipulate the existing connection
                // rather than creating a new one
                if (controller.getConnections(connector.getType()).
                        isInputConnected(connector)) {
                    return;
                }

                t.consume();
                newConnectionPressEvent = t;
            }
        });

        connectorNode.onMouseDraggedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                
                if (connectorNode.isMouseTransparent()) {
                    return;
                }

                // we are already connected and manipulate the existing connection
                // rather than creating a new one
                if (controller.getConnections(connector.getType()).
                        isInputConnected(connector)) {
                    return;
                }

                if (newConnectionSkin == null) {
                    newConnectionSkin
                            = new FXNewConnectionSkin(getSkinFactory(),
                                    getParent(), connector,
                                    getController(), connector.getType());

                    newConnectionSkin.add();

                    MouseEvent.fireEvent(
                            newConnectionSkin.getReceiverConnector(),
                            newConnectionPressEvent);
                }

                t.consume();
                MouseEvent.fireEvent(newConnectionSkin.getReceiverConnector(), t);

                t.consume();
                MouseEvent.fireEvent(newConnectionSkin.getReceiverConnector(), t);

            }
        });

        connectorNode.onMouseReleasedProperty().set(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                
                if (connectorNode.isMouseTransparent()) {
                    return;
                }

                connector.click(NodeUtil.mouseBtnFromEvent(t), t);

                // we are already connected and manipulate the existing connection
                // rather than creating a new one
                if (controller.getConnections(connector.getType()).
                        isInputConnected(connector)) {
                    return;
                }

                t.consume();
                try {
                    MouseEvent.fireEvent(newConnectionSkin.getReceiverConnector(), t);
                } catch (Exception ex) {
                    // TODO exception is not critical here (node already removed)
                }

                newConnectionSkin = null;
            }
        });
    }

    private void computeConnectorSizes() {

        double inset = 120;
        double minInset = 60;
        double minSize = 8;

        connectorSizes.clear();

        for (int i = 0; i < shapeLists.size(); i++) {
            List<Shape> shapeList = shapeLists.get(i);

            double connectorHeight = computeConnectorSize(inset, shapeList.size());

            if (connectorHeight < minSize) {
                double diff = minSize - connectorHeight;
                inset = Math.max(inset - diff * shapeList.size(), minInset);
                connectorHeight = computeConnectorSize(inset, shapeList.size());
            }

            connectorSizes.add(connectorHeight);
        }
    }

//    private void computeOutputConnectorSize() {
//        double inset = 120;
//        double minInset = 60;
//        double minSize = 8;
//
//        double connectorHeight = computeConnectorSize(inset, outputList.size());
//
//        if (connectorHeight < minSize) {
//            double diff = minSize - connectorHeight;
//            inset = Math.max(inset - diff * outputList.size(), minInset);
//            connectorHeight = computeConnectorSize(inset, outputList.size());
//        }
//
//        outputConnectorSize = connectorHeight;
//    }
    private double computeConnectorSize(double inset, int numConnectors) {

        if (numConnectors == 0) {
            return 0;
        }

        double maxSize = 10;

        double connectorHeight = maxSize * 2;
        double originalConnectorHeight = connectorHeight;
        double gap = 5;

        double totalHeight = numConnectors * connectorHeight
                + (numConnectors - 1) * gap;

        connectorHeight = Math.min(totalHeight,
                node.getPrefHeight() - inset) / (numConnectors);
        connectorHeight = Math.min(connectorHeight, maxSize * 2);

        if (numConnectors == 1) {
            connectorHeight = originalConnectorHeight;
        }

        return connectorHeight;
    }

    private double getMinConnectorSize() {
        Optional<Double> minSize = connectorSizes.stream().min(Double::compare);

        if (minSize.isPresent()) {
            return minSize.get();
        }

        return 0;
    }

    private List<Segment> nodeToSegments(VNode n) {
        List<Segment> result = new ArrayList<>();

        Vector2D np0 = new Vector2D(n.getX(), n.getY());
        Vector2D np1 = new Vector2D(n.getX() + n.getWidth(), n.getY());
        Vector2D np2 = new Vector2D(
                n.getX() + n.getWidth(),
                n.getY() + n.getHeight());
        Vector2D np3 = new Vector2D(n.getX(), n.getY() + n.getHeight());

        Line edge0 = new Line(np0, np1, 1e-5);
        Line edge1 = new Line(np1, np2, 1e-5);
        Line edge2 = new Line(np2, np3, 1e-5);
        Line edge3 = new Line(np3, np0, 1e-5);

        result.add(new Segment(np0, np1, edge0));
        result.add(new Segment(np1, np2, edge1));
        result.add(new Segment(np2, np3, edge2));
        result.add(new Segment(np3, np0, edge3));

        return result;
    }

    private Optional<Integer> getIntersectionIndex(
            Segment connectionSegment, List<Segment> edges) {

        SubLine connectionSegmentL = new SubLine(connectionSegment);

        int i = 0;
        for (Segment s : edges) {

            if (new SubLine(s).intersection(connectionSegmentL, true) != null) {
                return Optional.of(i);
            }

            i++;
        }

        return Optional.empty();
    }

    private Vector2D getNodeCenter(VNode n) {
        return new Vector2D(
                n.getX() + n.getWidth() * 0.5,
                n.getY() + n.getHeight() * 0.5);
    }

    private Segment getConnectionEdge(VNode sender, VNode receiver) {
        Vector2D senderC = getNodeCenter(sender);
        Vector2D receiverC = getNodeCenter(receiver);

        Line l = new Line(senderC, receiverC, 1e-5);

        return new Segment(senderC, receiverC, l);
    }

    private Pair<Integer, Integer> getConnectorEdges(Connection c) {

        VNode senderN = c.getSender().getNode();
        List<Segment> n1Edges = nodeToSegments(senderN);
        VNode receiverN = c.getReceiver().getNode();
        List<Segment> n2Edges = nodeToSegments(receiverN);

        Segment segment = getConnectionEdge(senderN, receiverN);

        Optional<Integer> senderIntersection
                = getIntersectionIndex(segment, n1Edges);
        Optional<Integer> receiverIntersection
                = getIntersectionIndex(segment, n2Edges);

        if (!senderIntersection.isPresent() || !receiverIntersection.isPresent()) {
            // rectangles overlap. therefore we use default layout
            return new Pair<>(RIGHT, LEFT);
        }

        return new Pair<>(senderIntersection.get(), receiverIntersection.get());
    }

    private void adjustConnectorSize() {

        double maxConnectorSize = Double.MAX_VALUE;

        Optional<Double> maxSize
                = getModel().getVisualizationRequest().
                get(VisualizationRequest.KEY_MAX_CONNECTOR_SIZE);

        if (maxSize.isPresent()) {
            maxConnectorSize = maxSize.get();
        }

        for (int i = 0; i < connectorSizes.size(); i++) {
            for (Shape connector : shapeLists.get(i)) {
                double size = connectorSizes.get(i);

                if (connector instanceof Circle) {

                    ((Circle) connector).setRadius(
                            Math.min(size * 0.5, maxConnectorSize * 0.5));
                }
            }
        }
    }

    private void removeConnector(Connector connector) {
        connectorList.remove(connector);
        Node connectorNode = connectors.remove(connector);

        if (connectorNode != null && connectorNode.getParent() != null) {

            // TODO: remove connectors&connections?
            if (connector.isInput()) {
                shapeLists.get(LEFT).remove(connectorNode);
            } else if (connector.isOutput()) {
                shapeLists.get(RIGHT).remove(connectorNode);
            }
            NodeUtil.removeFromParent(connectorNode);
        }
    }

    @Override
    public Window getNode() {
        return node;
    }

    @Override
    public Parent getContentNode() {
        return node.getWorkflowContentPane();
    }

    @Override
    public void remove() {
        removeSkinOnly = true;

        List<Connector> delList = new ArrayList<>(connectorList);

        for (Connector c : delList) {
            removeConnector(c);
        }
        if (node != null && node.getParent() != null) {
            NodeUtil.removeFromParent(node);
        }
        
        removeListeners(getModel());

        getModel().getVisualizationRequest().removeListener(vReqLister);
    }

    @Override
    public final void setModel(VNode model) {
        modelProperty.set(model);
    }

    @Override
    public final VNode getModel() {
        return modelProperty.get();
    }

    @Override
    public final ObjectProperty<VNode> modelProperty() {
        return modelProperty;
    }

    final void setParent(Parent parent) {
        parentProperty.set(parent);
    }

    Parent getParent() {
        return parentProperty.get();
    }

    ObjectProperty<Parent> parentProperty() {
        return parentProperty;
    }

    @Override
    public void add() {
        NodeUtil.addToParent(getParent(), node);
    }

    private void removeListeners(VNode flowNode) {
        flowNode.titleProperty().removeListener(modelTitleListener);
        flowNode.xProperty().removeListener(modelXListener);
        flowNode.yProperty().removeListener(modelYListener);
        flowNode.widthProperty().removeListener(modelWidthListener);
        flowNode.heightProperty().removeListener(modelHeightListener);

        node.layoutXProperty().removeListener(nodeXListener);
        node.layoutYProperty().removeListener(nodeXListener);
        node.prefWidthProperty().removeListener(nodeWidthListener);
        node.prefHeightProperty().removeListener(nodeHeightListener);
        
        flowNode.getVisualizationRequest().addListener(vReqLister);
    }

    private void initListeners() {
        modelTitleListener = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String oldVal, String newVal) {
                node.setTitle(newVal);
            }
        };

        modelXListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                node.setLayoutX((double) newVal);
            }
        };

        modelYListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                node.setLayoutY((double) newVal);
            }
        };

        modelWidthListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                node.setPrefWidth((double) newVal);
            }
        };

        modelHeightListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                node.setPrefHeight((double) newVal);
            }
        };

        nodeXListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                getModel().setX((double) newVal);
            }
        };

        nodeYListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                getModel().setY((double) newVal);
            }
        };

        nodeWidthListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                getModel().setWidth((double) newVal);
            }
        };

        nodeHeightListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldVal, Number newVal) {
                getModel().setHeight((double) newVal);
            }
        };

        node.onCloseActionProperty().set(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if (!removeSkinOnly) {
                    modelProperty().get().getFlow().remove(modelProperty().get());
                }
            }
        });
    }

    private void registerListeners(VNode flowNode) {

        initListeners();

        flowNode.titleProperty().addListener(modelTitleListener);
        flowNode.xProperty().addListener(modelXListener);
        flowNode.yProperty().addListener(modelYListener);
        flowNode.widthProperty().addListener(modelWidthListener);
        flowNode.heightProperty().addListener(modelHeightListener);

        node.layoutXProperty().addListener(nodeXListener);
        node.layoutYProperty().addListener(nodeYListener);
        node.prefWidthProperty().addListener(nodeWidthListener);
        node.prefHeightProperty().addListener(nodeHeightListener);
        
        initVReqListeners();

    }

    /**
     * @return the controller
     */
    @Override
    public VFlow getController() {
        return controller;
    }

    /**
     * @param controller the controller to set
     */
    @Override
    public void setController(VFlow controller) {
        this.controller = controller;
    }

    /**
     * @return the skinFactory
     */
    @Override
    public FXSkinFactory getSkinFactory() {
        return this.skinFactory;
    }

    public Node getConnectorNodeByReference(Connector c) {
        return connectors.get(c);
    }

    public void configureCanvas(VCanvas content) {
        //
    }
}

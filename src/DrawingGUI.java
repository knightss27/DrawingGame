import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DrawingGUI {

    GameClient gameClient;

    JFrame mainFrame = new JFrame("Drawing Game");
    JPanel mainPanel = new JPanel();
    DrawingPanel drawingPanel;
    ChatPanel chatPanel;
    HintPanel hintPanel;

    final int WIDTH = 700;
    final int HEIGHT = 480;

    BufferedImage mainImage = new BufferedImage(3*WIDTH/4, HEIGHT, BufferedImage.TYPE_INT_ARGB);;

    public boolean isAllowedToDraw = false;

    public DrawingGUI(GameClient gameClient) {
        this.gameClient = gameClient;
        drawingPanel = new DrawingPanel();
        chatPanel = new ChatPanel();
        hintPanel = new HintPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(drawingPanel, BorderLayout.CENTER);
        mainPanel.add(chatPanel, BorderLayout.EAST);
        mainPanel.add(hintPanel, BorderLayout.NORTH);
        generateFrame(mainPanel);

        drawingPanel.createBlank();
    }

    public void handleChat(Message<String> m) {
        chatPanel.messages.addFirst(m.data);
        chatPanel.repaint();
    }

    public void handleRound(Player drawingPlayer) {
        isAllowedToDraw = gameClient.player.equals(drawingPlayer);
        System.out.println("Drawing: " + drawingPlayer.id + ", is: " + gameClient.player.id + ", " + isAllowedToDraw);
        drawingPanel.createBlank();
    }

    public void handleHint(String hint) {
        hintPanel.currentWord = hint;
        hintPanel.repaint();
    }

    private class HintPanel extends JPanel {
        public String currentWord = "testing";

        public HintPanel() {
            setPreferredSize(new Dimension(3*DrawingGUI.this.WIDTH/4, DrawingGUI.this.HEIGHT/10));
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintBorder(g);

            Graphics2D pen = (Graphics2D) g;
            int x = getWidth()/2 - currentWord.length()/2 * 16;
            for (int i = 0; i < currentWord.length(); i++) {
                pen.drawString(currentWord.substring(i, i+1), x, getHeight()/2 + 5);
                x += 16;
            }
            pen.dispose();
        }
    }

    private class ChatPanel extends JPanel {

        LinkedList<String> messages;

        public ChatPanel() {
            setPreferredSize(new Dimension(DrawingGUI.this.WIDTH/4, DrawingGUI.this.HEIGHT));
            setLayout(new BorderLayout());
            add(new ChatDisplay(), BorderLayout.CENTER);
            add(new ChatField(), BorderLayout.SOUTH);

            setBorder(BorderFactory.createMatteBorder(0, 2, 2, 2, Color.LIGHT_GRAY));
            messages = new LinkedList<>();
        }

        private class ChatDisplay extends JPanel {
            public ChatDisplay() {
                setPreferredSize(new Dimension(DrawingGUI.this.WIDTH/4, DrawingGUI.this.HEIGHT));
                setBackground(new Color(238, 238, 238));
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int spacing = 22;
                int y = this.getHeight();
                int i = 0;
                Graphics2D pen = (Graphics2D) g;
                for (String m : messages) {
                    if ((messages.size()-i) % 2 == 0) {
                        pen.setColor(Color.WHITE);
                        pen.fillRect(0, y-spacing, getWidth(), spacing);
                    }

                    pen.setColor(Color.BLACK);
                    pen.drawString(m, 10, y-5);

                    y -= spacing;
                    i++;
                    if (y < 0) {
                        break;
                    }
                }
                pen.dispose();
            }
        }

        private class ChatField extends JTextField implements ActionListener {
            public ChatField() {
                super();
                setPreferredSize(new Dimension(DrawingGUI.this.WIDTH/4, DrawingGUI.this.HEIGHT/12));
                addActionListener(this);
                setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (this.getText().length() > 0) {
                    String toSend = this.getText();
                    gameClient.messagesToSend.add(new Message(111, Message.mType.CHAT, toSend));
                    this.setText("");
                }
            }
        }
    }

    public void handleNetworkedDrawing(int[] coordinates) {
        drawingPanel.handleNetworkedDrawing(coordinates);
    }

    private class DrawingPanel extends JPanel implements ComponentListener, MouseListener, MouseMotionListener {

        private int calculatedPhotoWidth = 0;
        private int calculatedPhotoHeight = 0;
        private int[] prevEvent;

        public DrawingPanel() {
            setPreferredSize(new Dimension((3*DrawingGUI.this.WIDTH)/4, DrawingGUI.this.HEIGHT));

            addComponentListener(this);
            addMouseListener(this);
            addMouseMotionListener(this);
            setBackground(new Color(0, 230, 230));
            setBorder(new BevelBorder(BevelBorder.LOWERED));

        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D pen = (Graphics2D) g;
            float width = getWidth();
            float height = getHeight();
            float imageAspect = (float) mainImage.getWidth() / mainImage.getHeight();

            if (width / height > imageAspect) {
                width = mainImage.getWidth() * (height / mainImage.getHeight());
            } else {
                height = mainImage.getHeight() * (width / mainImage.getWidth());
            }

            calculatedPhotoWidth = (int) width;
            calculatedPhotoHeight = (int) height;

            pen.drawImage(mainImage, 0, 0, (int) width, (int) height, this);
            pen.dispose();
        }

        private void createBlank() {
            mainImage = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D pen = mainImage.createGraphics();
            pen.setPaint(Color.white);
            pen.fillRect(0, 0, mainImage.getWidth(), mainImage.getHeight());
            pen.dispose();

            this.repaint();
        }

        public void handleNetworkedDrawing(int[] networkedData) {
            prevEvent = new int[]{networkedData[2], networkedData[3]};
            handleDrawing(new int[]{networkedData[0], networkedData[1]}, true);
        }

        private void handleDrawing(MouseEvent e) {
            handleDrawing(new int[]{e.getX(), e.getY()}, false);
        }

        private void handleDrawing(int[] coordinates, boolean isNetworked) {
            if (!isAllowedToDraw && !isNetworked) {
                return;
            }

            if (prevEvent == null) {
                prevEvent = coordinates;
            }

            int[] scaled = isNetworked ? coordinates : this.getScaledCoordinates(coordinates);
            int[] pastScaled = isNetworked ? prevEvent : this.getScaledCoordinates(prevEvent);

            if (scaled[0] > mainImage.getWidth() || scaled[1] > mainImage.getHeight()) {
                return;
            }

            Graphics2D pen = (Graphics2D) mainImage.getGraphics();
            pen.setColor(Color.BLACK); // TODO - add colors
            pen.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            pen.drawLine(scaled[0], scaled[1], pastScaled[0], pastScaled[1]);
            pen.dispose();
            this.repaint();

            if (isAllowedToDraw) {
                // Send to room
                gameClient.messagesToSend.add(new Message(111, Message.mType.DRAW, new int[]{scaled[0], scaled[1], pastScaled[0], pastScaled[1]}));
            }
        }

        public int[] getScaledCoordinates(int[] e) {
            float ratioX = (float) mainImage.getWidth() / calculatedPhotoWidth;
            float ratioY = (float) mainImage.getHeight() / calculatedPhotoHeight;
            int x = (int) (ratioX * e[0]);
            int y = (int) (ratioY * e[1]);
            return new int[]{x, y};
        }

        @Override
        public void componentResized(ComponentEvent e) {

        }

        @Override
        public void componentMoved(ComponentEvent e) {

        }

        @Override
        public void componentShown(ComponentEvent e) {

        }

        @Override
        public void componentHidden(ComponentEvent e) {

        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {
            prevEvent = new int[]{e.getX(), e.getY()};
            handleDrawing(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            prevEvent = new int[]{e.getX(), e.getY()};
            handleDrawing(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseDragged(MouseEvent e) {
            handleDrawing(e);
            prevEvent = new int[]{e.getX(), e.getY()};
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    }

    private void generateFrame(JPanel _main) {
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainFrame.add(_main);
        mainFrame.pack();

        mainFrame.setSize(WIDTH, HEIGHT);

        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

}

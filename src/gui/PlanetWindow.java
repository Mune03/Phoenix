/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import galaxyreader.Unit;
import game.Game;
import game.Hex;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import util.C;
import util.Util;
import util.WindowSize;

/**
 *
 * @author joulupunikki
 */
public class PlanetWindow extends JPanel {

    // pointer to GUI
    private Gui gui;
    private Game game;
    private int[][] hex_tiles;
    private WindowSize ws;
    JTextField planet_name_display;
    JTextField year_display;
    JTextField money_display;
    JTextField city_name;
    JLabel[] res_display;
    JButton end_turn;
    JButton next_stack;

    JButton skip_stack;

    JButton build;

    //bug test
    long test_long = 0;
//    long time;
//    boolean start = true;
//    JButton 
    public PlanetWindow(Gui gui) {
        this.gui = gui;
        ws = Gui.getWindowSize();

        game = gui.getGame();

        setUpInfoText();
        setUpButtons();
        setUpResDisplay();
//        setUpCoordinateListener(); // for testing positions on panel
    }

//    // for testing positions on panel
//    public void setUpCoordinateListener() {
//        this.addMouseListener(new MouseAdapter() {
//            public void mousePressed(MouseEvent e) {
////                clickOnPlanetMap(e);
//                Point p = e.getPoint();
//                System.out.println("Planet window (x,y): " + p.x + ", " + p.y);
//            }
//        });
//    }
    public void setGame(Game game) {
        this.game = game;
    }

    public void setUpButtons() {
        IndexColorModel color_index = gui.getICM();
        ButtonIcon end_turn_default = new ButtonIcon(ws.end_turn_button_w, ws.end_turn_button_h, "bin/efsbut16.bin", 0, color_index, ws);
        int file_offset = 2;
        ButtonIcon end_turn_pressed = new ButtonIcon(ws.end_turn_button_w, ws.end_turn_button_h, "bin/efsbut16.bin", file_offset, color_index, ws);
        end_turn = new JButton();
        end_turn.setBorder(null);
        end_turn.setIcon(end_turn_default);

        end_turn.setPressedIcon(end_turn_pressed);
        this.add(end_turn);
        end_turn.setBounds(ws.end_turn_button_x, ws.end_turn_button_y,
                ws.end_turn_button_w, ws.end_turn_button_h);
        end_turn.setEnabled(true);
        end_turn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getCurrentState().pressEndTurnButton();
            }
        });

        ButtonIcon next_stack_default = new ButtonIcon(ws.unit_order_buttons_w, ws.unit_order_buttons_h, "bin/efsbut10.bin", 0, color_index, ws);
        file_offset = 2;
        ButtonIcon next_stack_pressed = new ButtonIcon(ws.unit_order_buttons_w, ws.unit_order_buttons_h, "bin/efsbut10.bin", file_offset, color_index, ws);
        next_stack = new JButton();
        next_stack.setBorder(null);
        next_stack.setIcon(next_stack_default);

        next_stack.setPressedIcon(next_stack_pressed);
        this.add(next_stack);
        next_stack.setBounds(ws.unit_order_buttons_x, ws.unit_order_buttons_y,
                ws.unit_order_buttons_w, ws.unit_order_buttons_h);
        next_stack.setEnabled(true);
        next_stack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getCurrentState().pressNextStackButton();
            }
        });

        setUpSkipStack();

        ButtonIcon build_disabled = new ButtonIcon(ws.build_button_w, ws.build_button_h, null, 0, color_index, ws);
//        file_offset = 2;
//        ButtonIcon build_pressed = new ButtonIcon(ws.unit_order_buttons_w, ws.unit_order_buttons_h, "bin/efsbut10.bin", file_offset, color_index, ws);
        build = new JButton("Build");
        build.setFont(ws.font_default);
        build.setBorder(BorderFactory.createLineBorder(C.COLOR_GOLD));
//        build.setIcon(next_stack_default);
        build.setDisabledIcon(build_disabled);
//        build.setPressedIcon(next_stack_pressed);
        this.add(build);
        build.setBounds(ws.build_button_x_offset, ws.build_button_y_offset,
                ws.build_button_w, ws.build_button_h);
//        build.setEnabled(true);
        build.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getCurrentState().pressBuildButton();
            }
        });

    }

    public void setUpSkipStack() {
        IndexColorModel color_index = gui.getICM();
        String file_name = "bin/efsbut9.bin";
        ButtonIcon skip_stack_default = new ButtonIcon(ws.unit_order_buttons_w, ws.unit_order_buttons_h, file_name, 0, color_index, ws);
        int file_offset = 2;
        ButtonIcon skip_stack_pressed = new ButtonIcon(ws.unit_order_buttons_w, ws.unit_order_buttons_h, file_name, file_offset, color_index, ws);
        skip_stack = new JButton();
        skip_stack.setBorder(null);
        skip_stack.setIcon(skip_stack_default);

        skip_stack.setPressedIcon(skip_stack_pressed);
        this.add(skip_stack);
        skip_stack.setBounds(ws.unit_order_buttons_x, ws.unit_order_buttons_y + ws.unit_order_buttons_h,
                ws.unit_order_buttons_w, ws.unit_order_buttons_h);
        skip_stack.setEnabled(true);
        skip_stack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.getCurrentState().pressSkipStackButton();
            }
        });
    }

    public void setUpInfoText() {
        planet_name_display = new JTextField();
        year_display = new JTextField();
        money_display = new JTextField();
        this.add(planet_name_display);
        planet_name_display.setBounds(ws.info_text_field_x, ws.info_text_field_y,
                ws.info_text_field_w, ws.info_text_field_h);
        planet_name_display.setBackground(Color.BLACK);
        planet_name_display.setForeground(C.COLOR_GOLD);
        planet_name_display.setEditable(false);
        planet_name_display.setHorizontalAlignment(JTextField.CENTER);
        planet_name_display.setBorder(null);
        planet_name_display.setFont(ws.font_default);

        this.add(year_display);
        year_display.setBounds(ws.info_text_field_x, ws.info_text_field_y + ws.info_text_field_h,
                ws.info_text_field_w, ws.info_text_field_h);
        year_display.setBackground(Color.BLACK);
        year_display.setForeground(C.COLOR_GOLD);
        year_display.setEditable(false);
        year_display.setHorizontalAlignment(JTextField.CENTER);
        year_display.setBorder(null);
        year_display.setFont(ws.font_default);

        this.add(money_display);
        money_display.setBounds(ws.info_text_field_x, ws.info_text_field_y + 2 * ws.info_text_field_h,
                ws.info_text_field_w, ws.info_text_field_h);
        money_display.setBackground(Color.BLACK);
        money_display.setForeground(C.COLOR_GOLD);
        money_display.setEditable(false);
        money_display.setHorizontalAlignment(JTextField.CENTER);
        money_display.setBorder(null);
        money_display.setFont(ws.font_default);

        city_name = new JTextField();

        this.add(city_name);
        city_name.setBounds(ws.info_text_field_x, ws.info_text_field_y + (int) (3.5 * ws.info_text_field_h),
                ws.info_text_field_w, ws.info_text_field_h);
        city_name.setBackground(Color.BLACK);
        city_name.setForeground(C.COLOR_GOLD);
        city_name.setEditable(false);
        city_name.setHorizontalAlignment(JTextField.CENTER);
        city_name.setBorder(null);
        city_name.setFont(ws.font_default);

    }

    public void setUpResDisplay() {
        res_display = new JLabel[C.RES_TYPES];
        for (int i = 0; i < res_display.length; i++) {
            res_display[i] = new JLabel();
            this.add(res_display[i]);
            res_display[i].setBounds(ws.pw_res_display_x_offset + i * ws.pw_res_display_x_gap, ws.pw_res_display_y_offset, ws.pw_res_display_w, ws.pw_res_display_h);
//            res_display[i].setBackground(Color.WHITE);
            res_display[i].setOpaque(false);
            res_display[i].setForeground(C.COLOR_RES_DISP_GREEN);
//            res_display[i].setEditable(false);
            res_display[i].setHorizontalAlignment(JLabel.CENTER);
            res_display[i].setBorder(null);
            res_display[i].setFont(ws.font_default);
            res_display[i].setText("123");
        }

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        renderPlanetWindow(g);
//        System.out.println("Repaint planet window " + test_long++ + ", stack move counter = " + gui.getStackMoveCounter());
//        test_long++;
//        if (start) {
//            time = System.currentTimeMillis();
//            start = false;
//        } else if (test_long == 1000) {
//            System.out.println("" + test_long + " paints in " + (System.currentTimeMillis() - time) + " ms.");
//        }
    }

    public void renderPlanetWindow(Graphics g) {

        byte[][] pallette = gui.getPallette();
        BufferedImage bi = Util.loadImage("pcx/plnplat3.pcx", ws.is_double, pallette, 640, 480);
        drawResourceIcons(bi.getRaster());

        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(bi, null, 0, 0);

        Point selected = game.getSelectedPoint();
        int faction = game.getSelectedFaction();
        boolean enable_launch = false;
        boolean enable_build = false;
        if (selected != null) {
            Util.drawStackDisplay(g, game, selected, faction);

            if (faction == -1) {
                Hex hex = game.getPlanetGrid(game.getCurrentPlanetNr()).getHex(selected.x, selected.y);
                if (hex.getStructure() != null) {
                    city_name.setText(game.getStrBuild(hex.getStructure().type).name);
                } else {
                    city_name.setText("");
                }
                List<Unit> stack = game.getPlanetGrid(game.getCurrentPlanetNr()).getHex(selected.x, selected.y).getStack();
                if (stack.get(0).owner == game.getTurn()) {
//                    Hex hex = game.getPlanetGrid(game.getCurrentPlanetNr()).getHex(selected.x, selected.y);

                    if (hex.getStructure() != null) {
                        enable_build = true;
                    }

                    for (Unit unit : stack) {
                        if (unit.selected) {
                            if ((unit.move_type == C.MoveType.JUMP
                                    || unit.move_type == C.MoveType.LANDER
                                    || unit.move_type == C.MoveType.SPACE)
                                    && unit.move_points > 0) {
                                enable_launch = true;
                            } else {
                                enable_launch = false;
                                break;
                            }
                        }
                    }
                }
            }
        }

        gui.enableLaunchButton(enable_launch);
        if (enable_build) {
            build.setBorder(C.GOLD_BORDER);
        } else {
            build.setBorder(null);
        }

        build.setEnabled(enable_build);

        planet_name_display.setText(game.getPlanet(game.getCurrentPlanetNr()).name);
        year_display.setText(
                "A.D. " + game.getYear());
        money_display.setText(
                "4500 FB");
        drawResAmounts();
    }

    public void drawResAmounts() {
        int[] res_amounts = game.getResources().getResourcesAvailable(game.getCurrentPlanetNr(), game.getTurn());
        int[] balance = game.getResources().getProdConsBalance(game.getTurn(), game.getCurrentPlanetNr());
        for (int i = 0; i < res_display.length; i++) {
            if (balance[i] < 0) {
                res_display[i].setForeground(Color.RED);
            } else {
                res_display[i].setForeground(C.COLOR_RES_DISP_GREEN);
            }
            res_display[i].setText(Util.c4Display(res_amounts[i]));

        }
    }

//    public void drawResAmounts2() {
//        Font font = ws.font_default;
//        FontMetrics fm = new FontMetrics(font);
//
//    }
    public void drawResourceIcons(WritableRaster wr) {
        int[][] res_icons = gui.getResources().getResIcons();
        int x = 128;
        int y = 442;
        int x_offset = 38;
        int[] pixel_data = new int[1];
        int w = C.CARGO_WIDTH;
        int h = C.CARGO_HEIGHT;

        for (int i = 0; i < res_icons.length; i++) {
            Util.writeImage(pixel_data, i, res_icons,
                    wr, ws, w, h, x + i * x_offset, y);

        }
    }

}

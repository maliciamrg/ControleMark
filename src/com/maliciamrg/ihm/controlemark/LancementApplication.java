package com.maliciamrg.ihm.controlemark;

import javax.swing.*;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class LancementApplication extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -762861862628625989L;
	private static String workRepertoire;
	private static String username;
	/** The Fileseparator. */
	public static String Fileseparator = "/";
	private static ArrayList<String> domainelist;
	private static Connection con;
	private static JFrame fenetre;

	public LancementApplication() {
		super("titre de l'application");

		WindowListener l = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		};

		addWindowListener(l);
		setSize(200, 100);
		setVisible(true);
	}

	public static void main(String[] args) throws ClassNotFoundException, SQLException {

		param();

		domainelist = new ArrayList<String>();
		fenetre = new LancementApplication();
		fenetre.setTitle("Rapport controle Mark");
		fenetre.setSize(700, 1000);

		((LancementApplication) fenetre).majFenetre();

		// JLabel label = new JLabel("Bonjour tout le monde !");
		// fenetre.getContentPane().add(label);

		fenetre.setVisible(true);
	}

	public void majFenetre() throws SQLException {
		JPanel pannel = new JPanel();

		String host = "jdbc:hsqldb:F:\\workspace\\ControleMark\\src\\HSQLDB";
		Connection conn = DriverManager.getConnection(host);
		
		
		
		con = connect(workRepertoire + "ControleMark.accdb");

		Statement stmtdtmaj = con.createStatement();

		// recuperer dtmaj
		String dtmaj = "";
		ResultSet rsdtmaj = stmtdtmaj.executeQuery("select valeur FROM rst_date where champ = 'datemaj' ");
		while (rsdtmaj.next()) {
			dtmaj = rsdtmaj.getString("valeur");
		}

		pannel.add(new JLabel("last update : " + dtmaj));
		pannel.add(new JLabel(workRepertoire + "ControleMark.accdb"));

		JTabbedPane onglets = new JTabbedPane(SwingConstants.TOP);

		onglets = ((LancementApplication) fenetre).creationOnglets();

		onglets.setOpaque(true);
		pannel.add(onglets);
		fenetre.getContentPane().removeAll();
		fenetre.getContentPane().add(pannel);
		fenetre.setVisible(true);

		fenetre.getContentPane().add(pannel);
	}

	private JTabbedPane creationOnglets() throws SQLException {
		JTabbedPane onglets = new JTabbedPane(SwingConstants.TOP);

		Statement stmtdomaine = con.createStatement();
		ResultSet rsdomaine = stmtdomaine
				.executeQuery("select domaine , count(*) as nbd FROM E500_resultatpardomainapresexclusion group by domaine order by domaine asc");
		while (rsdomaine.next()) {

			String domaine = rsdomaine.getString("domaine");
			String nbd = rsdomaine.getString("nbd");
			String nom;
			if (domaine == null) {
				nom = "Sans domaine" + " (" + nbd + ")";
			} else {
				nom = domaine + " (" + nbd + ")";
				domainelist.add(domaine);
			}
			String rqtdomaine = (domaine == null ? (" domaine is NULL") : (" domaine = '" + domaine + "'"));

			JPanel onglet = new JPanel();
			// JLabel titreOnglet1 = new JLabel(nom);
			// onglet.add(titreOnglet1);

			Statement stmtbriqueversion = con.createStatement();
			ResultSet rsbriqueversion = stmtbriqueversion
					.executeQuery("select brique , version , denomination , count(*) as nbbv FROM E500_resultatpardomainapresexclusion where " + rqtdomaine
							+ " group by brique , version , denomination order by brique , version ");

			// create the root node
			DefaultMutableTreeNode root = new DefaultMutableTreeNode(nom);
			final JTree tree = new JTree(root);

			while (rsbriqueversion.next()) {
				String brique = rsbriqueversion.getString("brique");
				String version = rsbriqueversion.getString("version");
				String denomination = rsbriqueversion.getString("denomination");
				String nbbv = rsbriqueversion.getString("nbbv");

				DefaultMutableTreeNode domaineNode = new DefaultMutableTreeNode(brique + " : " + version + " - " + denomination + " (" + nbbv + ")");

				Statement stmtcomposants = con.createStatement();
				ResultSet rscomposants = stmtcomposants.executeQuery("select composant , mark FROM E500_resultatpardomainapresexclusion where " + " brique = '"
						+ brique + "'" + " and version = '" + version + "'" + "and " + rqtdomaine + " group by  composant , mark order by composant , mark");

				String composantsprev = "";
				DefaultMutableTreeNode nodecomposant = null;
				while (rscomposants.next()) {

					String composant = rscomposants.getString("composant");
					String mark = rscomposants.getString("mark");

					if (!composant.equals(composantsprev)) {
						nodecomposant = new DefaultMutableTreeNode(composant);
						domaineNode.add(nodecomposant);
					}
					nodecomposant.add(new DefaultMutableTreeNode(mark));

					composantsprev = composant;

				}

				root.add(domaineNode);

			}

			tree.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e)) {
						TreePath path = tree.getPathForLocation(e.getX(), e.getY());
						Rectangle pathBounds = tree.getUI().getPathBounds(tree, path);
						if (pathBounds != null && pathBounds.contains(e.getX(), e.getY())) {
							JPopupMenu menu = nouveaumenu(tree, path, domainelist);
							menu.show(tree, pathBounds.x, pathBounds.y + pathBounds.height);
						}
					}
					if (SwingUtilities.isLeftMouseButton(e)) {
						TreePath path = tree.getPathForLocation(e.getX(), e.getY());
						Rectangle pathBounds = tree.getUI().getPathBounds(tree, path);
						if (pathBounds != null && pathBounds.contains(e.getX(), e.getY())) {
							JPopupMenu menu = nouveaumenuLeft(tree, path);
							menu.show(tree, pathBounds.x, pathBounds.y + pathBounds.height);
						}
					}
				}
			});




			tree.setCellRenderer(new TooltipTreeRenderer());
			javax.swing.ToolTipManager.sharedInstance().registerComponent(tree);
			JScrollPane jpan = new JScrollPane(tree);
			jpan.setPreferredSize(new Dimension(660, 900));
			onglet.add(jpan);
			onglet.setPreferredSize(new Dimension(680, 950));

			onglets.addTab(nom, onglet);

		}
		return onglets;
	}

	private static void param() {
		workRepertoire = System.getProperty(("user.dir")) + Fileseparator;
		workRepertoire = "T:\\CND\\ENT\\20-FABRICATION\\30 - Réalisation\\Normes de programmation\\Etudes Source-Mark" + Fileseparator;
		workRepertoire = workRepertoire.replace(Fileseparator + Fileseparator, Fileseparator);
		username = System.getProperty("user.name");
	}

	public static Connection connect(String filename) {
		Connection conn = null;
		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");

			// C:\\databaseFileName.accdb" - location of your database
			String url = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb, *.accdb)};DBQ=" + filename;
			// + "E:\\workspace\\ControleMark\\ControleMark.accdb";

			// specify url, username, pasword - make sure these are valid
			conn = DriverManager.getConnection(url, "username", "password");

			System.out.println("Connection Succesfull");
		} catch (Exception e) {
			System.err.println("Got an exception! ");
			System.err.println(e.getMessage());

		}
		return conn;
	}

	private static JPopupMenu nouveaumenu(JTree tree, TreePath path, ArrayList<String> dmlist) {

		ActionListener menuListenern001 = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String composant = (String) ((JMenuItem) event.getSource()).getClientProperty("composant");
				String version = (String) ((JMenuItem) event.getSource()).getClientProperty("version");
				String mark = (String) ((JMenuItem) event.getSource()).getClientProperty("mark");
				TreePath path = (TreePath) ((JMenuItem) event.getSource()).getClientProperty("path");
				JTree tree = (JTree) ((JMenuItem) event.getSource()).getClientProperty("tree");
				Statement stmt = null;

				try {
					stmt = con.createStatement();
					try {
						stmt.execute("INSERT INTO  util_exclusion VALUES  ( '" + composant + "' , '" + mark + "' , '" + version + "' , '" + username + "' , '"
								+ (new SimpleDateFormat("yyyy-MM-dd")).format(Calendar.getInstance().getTime()) + "')");
					} catch (SQLException e1) {
						try {
							stmt.execute("UPDATE util_exclusion SET versiondesupression = '" + version + "' , userautorité = '" + username
									+ "' , commentaire = '" + (new SimpleDateFormat("yyyy-MM-dd")).format(Calendar.getInstance().getTime())
									+ "' WHERE composant = '" + composant + "' and mark = '" + mark + "'");
						} catch (SQLException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				DefaultMutableTreeNode node;
				DefaultTreeModel model = (DefaultTreeModel) (tree.getModel());
				node = (DefaultMutableTreeNode) (path.getLastPathComponent());
				model.removeNodeFromParent(node);
			}
		};
		ActionListener menuListenern002 = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String composant = (String) ((JMenuItem) event.getSource()).getClientProperty("composant");
				String version = (String) ((JMenuItem) event.getSource()).getClientProperty("version");
				TreePath path = (TreePath) ((JMenuItem) event.getSource()).getClientProperty("path");
				JTree tree = (JTree) ((JMenuItem) event.getSource()).getClientProperty("tree");
				Statement stmt = null;

				try {
					stmt = con.createStatement();
					try {
						stmt.execute("INSERT INTO  util_miseaecart VALUES  ( '" + composant + "' , '" + version + "' , '" + username + "' , '"
								+ (new SimpleDateFormat("yyyy-MM-dd")).format(Calendar.getInstance().getTime()) + "')");
					} catch (SQLException e1) {
						try {
							stmt.execute("UPDATE util_miseaecart SET versiondesupression = '" + version + "' , userautorité = '" + username
									+ "' , commentaire = '" + (new SimpleDateFormat("yyyy-MM-dd")).format(Calendar.getInstance().getTime())
									+ "' WHERE composant = '" + composant + "' ");
						} catch (SQLException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				DefaultMutableTreeNode node;
				DefaultTreeModel model = (DefaultTreeModel) (tree.getModel());
				node = (DefaultMutableTreeNode) (path.getLastPathComponent());
				model.removeNodeFromParent(node);
			}
		};
		ActionListener menuListenern003 = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String composant = (String) ((JMenuItem) event.getSource()).getClientProperty("composant");
				String domaine = (String) ((JMenuItem) event.getSource()).getClientProperty("domaine");
				TreePath path = (TreePath) ((JMenuItem) event.getSource()).getClientProperty("path");
				JTree tree = (JTree) ((JMenuItem) event.getSource()).getClientProperty("tree");
				Statement stmt = null;

				try {
					stmt = con.createStatement();
					try {
						stmt.execute("INSERT INTO  utili_liencomposantdomaine VALUES  ( '" + composant + "' , '" + domaine + "')");
					} catch (SQLException e1) {
						try {
							stmt.execute("UPDATE utili_liencomposantdomaine SET domaine = '" + domaine + "' WHERE composant = '" + composant + "'");
						} catch (SQLException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				DefaultMutableTreeNode node;
				DefaultTreeModel model = (DefaultTreeModel) (tree.getModel());
				node = (DefaultMutableTreeNode) (path.getLastPathComponent());
				model.removeNodeFromParent(node);

			}
		};

		JPopupMenu menu = new JPopupMenu();
		String mark = "";
		String brique = "";
		String version = "";
		String composant = "";
		String domaine = "";
		switch (path.getPathCount()) {
		case 4:
			mark = path.getPathComponent(3).toString();
		case 3:
			composant = path.getPathComponent(2).toString();
		case 2:
			brique = path.getPathComponent(1).toString().substring(0, 6);
			version = path.getPathComponent(1).toString().substring(9, 14);
		case 1:
			domaine = path.getPathComponent(0).toString();
		}

		JMenuItem item;
		// menu.add(new JMenuItem(path.toString()));
		switch (path.getPathCount()) {
		case 4:
			item = new JMenuItem("Le couple (" + composant + " " + mark + ") a etait refondu/disparu depuis la " + version);
			item.putClientProperty("composant", composant);
			item.putClientProperty("version", version);
			item.putClientProperty("mark", mark);
			item.putClientProperty("path", path);
			item.putClientProperty("tree", tree);
			menu.add(item);
			item.addActionListener(menuListenern001);
			menu.add(new JMenuItem("---------------------------------"));
		case 3:
			item = new JMenuItem("Le composant " + composant + " de la " + version + " ne doit plus apparaitre dans les controles futur");
			item.putClientProperty("composant", composant);
			item.putClientProperty("version", version);
			item.putClientProperty("path", path);
			item.putClientProperty("tree", tree);
			item.addActionListener(menuListenern002);
			menu.add(item);
			item.addActionListener(menuListenern002);
			menu.add(new JMenuItem("---------------------------------"));
			for (String dm : dmlist) {
				if (!dm.equals(domaine.substring(0, dm.length()))) {
					item = new JMenuItem("Le composant " + composant + " doit etre assigné au domaine " + dm);
					item.putClientProperty("composant", composant);
					item.putClientProperty("domaine", dm);
					item.putClientProperty("path", path);
					item.putClientProperty("tree", tree);
					menu.add(item);
					item.addActionListener(menuListenern003);
				}
			}
		case 2:
		case 1:
		}
		return menu;
	}

	private static JPopupMenu nouveaumenuLeft(JTree tree, TreePath path) {

		JPopupMenu menu = new JPopupMenu();
		String mark = "";
		String brique = "";
		String version = "";
		String composant = "";
		String domaine = "";
		switch (path.getPathCount()) {
		case 4:
			mark = path.getPathComponent(3).toString();
		case 3:
			composant = path.getPathComponent(2).toString();
		case 2:
			brique = path.getPathComponent(1).toString().substring(0, 6);
			version = path.getPathComponent(1).toString().substring(9, 14);
		case 1:
			domaine = path.getPathComponent(0).toString();
		}

		JMenuItem item;	
		// menu.add(new JMenuItem(path.toString()));
		switch (path.getPathCount()) {
		case 4:
			String text = stringtableaumarkmanquante(brique, composant, mark);
			item = new JMenuItem(text);
			menu.add(item);
			break;
		case 3:
		case 2:
		case 1:
		}
		return menu;
	}

	private static String stringtableaumarkmanquante(String brique, String composant, String mark) {
		String cle = "";
		String clepre = "";
		String ret = "<html>";
		try {
			Statement stmttableaumarkmanquante = con.createStatement();
			ResultSet rstableaumarkmanquante = stmttableaumarkmanquante.executeQuery(""
					+ " SELECT distinct numeroordre, version ,denomination ,stage , mark  " + " FROM rst_repartitionmark WHERE brique = '" + brique
					+ "' and composant = '" + composant + "' and mark = '" + mark + "' " + " group by numeroordre ,version ,denomination ,stage  , mark  "
					+ " union " + " SELECT distinct numeroordre, version ,denomination ,stage , '' as mark " + " FROM rst_repartitionmark WHERE brique = '"
					+ brique + "' and composant = '" + composant + "' " + " group by numeroordre, version ,denomination ,stage  , mark  "
					+ " order by numeroordre asc , mark desc");

			while (rstableaumarkmanquante.next()) {
				String version = rstableaumarkmanquante.getString("version");
				String denomination = rstableaumarkmanquante.getString("denomination");
				String stage = rstableaumarkmanquante.getString("stage");
				String marklu = rstableaumarkmanquante.getString("mark");
				cle = version + denomination + stage;
				// mise en forme
				if (!cle.equals(clepre)) {
					ret = ret + "<pre>" + version + "  " + (denomination + "                          ").substring(0, 25) + "  " + stage + "  " + marklu
							+ "</pre>";
				}
				clepre = cle;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ret = ret + "</html>";
		return ret;
	}

	public class TooltipTreeRenderer extends DefaultTreeCellRenderer {
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			final Component rc = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			if (hasFocus) {
				// ((DefaultMutableTreeNode)value).getDepth();
				// TreePath path2 = tree.getPathForRow(row);
				// System.out.println(String.valueOf(sel) + ":" +
				// String.valueOf(expanded) + ":" + String.valueOf(leaf) + ":" +
				// String.valueOf(hasFocus) + ":"
				// + ((DefaultMutableTreeNode) value).getDepth() + "-" +
				// value.toString());
				String tooltip = "";
				this.setToolTipText(tooltip);
				String brique;
				String composant;
				String mark;
				switch (((DefaultMutableTreeNode) value).getDepth()) {
				case 3:
					tooltip = "Domaine";
					this.setToolTipText(tooltip);
					break;
				case 2:
					tooltip = "Version où une Mark manque";
					this.setToolTipText(tooltip);
					break;
				case 1:
					tooltip = "Composants où une Mark manque";
					this.setToolTipText(tooltip);
					break;
				case 0:
					break;
				default:
					this.setToolTipText(tooltip);
					break;
				}
			}
			return rc;
		}

	}
}

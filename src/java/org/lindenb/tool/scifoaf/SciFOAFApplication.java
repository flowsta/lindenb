package org.lindenb.tool.scifoaf;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.lindenb.io.PreferredDirectory;
import org.lindenb.lang.RunnableObject;
import org.lindenb.lang.ThrowablePane;
import org.lindenb.sw.model.DerbyModel;
import org.lindenb.sw.model.DerbyModel.CloseableIterator;
import org.lindenb.sw.model.DerbyModel.RDFNode;
import org.lindenb.sw.model.DerbyModel.Resource;
import org.lindenb.sw.model.DerbyModel.Statement;
import org.lindenb.sw.vocabulary.DC;
import org.lindenb.sw.vocabulary.FOAF;
import org.lindenb.sw.vocabulary.Geo;
import org.lindenb.sw.vocabulary.Namespace;
import org.lindenb.sw.vocabulary.RDF;
import org.lindenb.swing.ConstrainedAction;
import org.lindenb.swing.DocumentAdapter;
import org.lindenb.swing.ObjectAction;
import org.lindenb.swing.SimpleDialog;
import org.lindenb.swing.SwingUtils;
import org.lindenb.swing.layout.InputLayout;
import org.lindenb.swing.table.GenericTableModel;

import org.lindenb.util.Compilation;
import org.lindenb.util.Debug;
import org.lindenb.util.Observed;
import org.lindenb.util.Pair;
import org.lindenb.util.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class GeoName
	{
	String name;
	String postalCode;
	String country;
	Double latitude;
	Double longitude;
	String adminCode1;
	String adminName1;
	String adminCode2;
	String adminName2;
	}

class GeoNamePane extends SimpleDialog
	{
	private static final long serialVersionUID = 1L;
	public static final String HEADERS[]={
	        "Postal Code","Name","Country","Lat","Long",
	        "Admin Code 1","Admin Name 1",
	        "Admin Code 2","Admin Name 2"
	        };
	private JTextField postCodeField;
	JTextField placeNameField;
	private JTextField countryField;
	private SpinnerNumberModel maxRows;
	private Thread thread=null;
	private JTable table=null;
	private GenericTableModel<GeoName> tableModel;
	private AbstractAction goAction;
	public GeoName geoNameSelected=null;
	private class SearchPlace implements Runnable
	        {
	        public void run() {
	                try {
	                        StringBuilder urlStr=new StringBuilder(
	                                        "http://ws.geonames.org/postalCodeSearch?style=FULL"
	                                        );
	                        String s= postCodeField.getText();
	                        if(s.trim().length()>0)
	                                {
	                                urlStr.append("&postalcode="+URLEncoder.encode(s, "UTF-8"));
	                                }
	                        s= placeNameField.getText();
	                        if(s.trim().length()>0)
	                                {
	                                urlStr.append("&placename="+URLEncoder.encode(s, "UTF-8"));
	                                }
	                        s= countryField.getText();
	                        if(s.trim().length()>0)
	                                {
	                                urlStr.append("&country="+URLEncoder.encode(s, "UTF-8"))
	;
	                                }
	                        urlStr.append("&maxRows="+maxRows.getNumber().intValue());
	                        Vector<GeoName> geoNames= new Vector<GeoName>(maxRows.getNumber().intValue(),1);
	                        
	                        DocumentBuilderFactory f= DocumentBuilderFactory.newInstance();
	                        DocumentBuilder b= f.newDocumentBuilder();
	                        Document doc= b.parse(urlStr.toString());
	                        Element root=doc.getDocumentElement();
	                        if(root==null) return;
	                        
	                        for(Node n1= root.getFirstChild();n1!=null;n1=n1.getNextSibling(
	))
	                                {
	                                if(n1.getNodeType()!=Node.ELEMENT_NODE) continue;
	                                if(!n1.getNodeName().equals("code")) continue;
	                                GeoName newRow= new GeoName();
	                                for(Node n2= n1.getFirstChild();n2!=null;n2=n2.getNextSibling())
	                                        {
	                                        if(n2.getNodeType()!=Node.ELEMENT_NODE) continue;
	                                        s= n2.getNodeName();
	                                        String txt=n2.getTextContent();
	                                       if(s.equals("postalcode")) newRow.postalCode=txt;
	                                        else if(s.equals("name")) newRow.name=txt;
	                                        else if(s.equals("countryCode")) newRow.country=txt;
	                                        else if(s.equals("lat")) newRow.latitude=new Double(txt);
	                                        else if(s.equals("lng")) newRow.longitude=new Double(txt);
	                                        else if(s.equals("adminCode1")) newRow.adminCode1=txt;
	                                        else if(s.equals("adminName1")) newRow.adminName1=txt;
	                                        else if(s.equals("adminCode2")) newRow.adminCode2=txt;
	                                        else if(s.equals("adminName2")) newRow.adminName2=txt;
	                                        }
	                                geoNames.addElement(newRow);
	                                }
	                        
	                        synchronized (tableModel)
	                        	{
								tableModel.clear();
								tableModel.addAll(geoNames);
	                        	}
	
	                        } catch (Exception e) {
	
	                        }
	                thread=null;
	                goAction.setEnabled(true);
	                }
	        }
	
	public GeoNamePane(Component owner)
	        {
			super(owner,"GeoName");
			this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			
			
	        JPanel contentPane=new JPanel(new BorderLayout(5,5));
	        getContentPane().add(contentPane);
	        this.goAction=new ConstrainedAction<GeoNamePane>(this,"Go")
	                {
	                private static final long serialVersionUID = 1L;
	                public void actionPerformed(ActionEvent e)
	                        {
	                        searchPlace();
	                        }
	                };
	        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEADING));
	        contentPane.add(top,BorderLayout.NORTH);
	        JLabel label=new JLabel("Postal Code:",JLabel.RIGHT);
	
	        top.add(label);
	        this.postCodeField= new JTextField("",10);
	        this.postCodeField.addActionListener(goAction);
	        top.add(this.postCodeField);
	        label=new JLabel("Place Name:",JLabel.RIGHT);
	        top.add(label);
	        this.placeNameField= new JTextField("",20);
	        this.placeNameField.addActionListener(goAction);
	        top.add(this.placeNameField);
	        label=new JLabel("Country:",JLabel.RIGHT);
	        top.add(label);
	        this.countryField= new JTextField("",3);
	        top.add(this.countryField);
	        label=new JLabel("Max Rows:",JLabel.RIGHT);
	        top.add(label);
	        top.add(new JSpinner(this.maxRows=new SpinnerNumberModel(10,1,50,1)));
	        top.add(new JButton(goAction));
	
	        this.table= new JTable( this.tableModel= new GenericTableModel<GeoName>()
	        	{
	            private static final long serialVersionUID = 1L;
	            @Override
	            public int getColumnCount() {
	            	return HEADERS.length;
	            	}
	            @Override
	            public String getColumnName(int column) {
	            	return HEADERS[column];
	            	}
	            @Override
	            public Object getValueOf(GeoName geo, int columnIndex)
	            	{
	            	switch(columnIndex)
		            	{
		            	case 0: return  geo.postalCode;
		            	case 1: return  geo.name;
		            	case 2: return  geo.country;
		            	case 3: return  geo.latitude;
		            	case 4: return  geo.longitude;
		            	case 5: return  geo.adminCode1;
		            	case 6: return  geo.adminName1;
		            	case 7: return  geo.adminCode2;
		            	case 8: return geo.adminName2;
		            	}
	            	return null;
	            	}
	           
	            });
	        this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
	        	{
	        	@Override
	        	public void valueChanged(ListSelectionEvent e) {
	        		int i= table.getSelectedRow();
	        		if(i==-1)
	        			{
	        			geoNameSelected=null;
	        			}
	        		else
	        			{
	        			geoNameSelected = tableModel.elementAt(i);
	        			}
	        		}
	        	});
	        contentPane.add(new JScrollPane(this.table),BorderLayout.CENTER);
	        getOKAction().mustHaveOneRowSelected(this.table);
	        }
	
	private void searchPlace()
	        {
	        if(this.thread!=null) return;
	        this.goAction.setEnabled(false);
	        SearchPlace run=new SearchPlace();
	        this.thread=new Thread(run);
	        this.thread.start();
	        }
		}


/**
 * 
 * SpatialThingEditor
 *
 */
abstract class SpatialThingEditor extends SimpleDialog
	{
	private static final long serialVersionUID = 1L;
	Vector<Pair<FoafModel.Resource,JComponent>> fields= new Vector<Pair<Resource,JComponent>>();
	
	JTextField placeName;
	JTextField latitude;
	JTextField longitude;
	public SpatialThingEditor(Component owner)
		{
		super(owner,Geo.NS+"SpatialThing");
		JPanel main= new JPanel(new BorderLayout(2,2));
		getContentPane().add(main);
		JPanel inputPane= new JPanel(new InputLayout());
		main.add(inputPane,BorderLayout.CENTER);
		
		inputPane.add(new JLabel("Place Name:",JLabel.RIGHT));
		inputPane.add(this.placeName= new JTextField(20));
		fields.add(new Pair<FoafModel.Resource, JComponent>(getModel().createResource(DC.NS+"title"),this.placeName));
		
		inputPane.add(new JLabel("Latitude:",JLabel.RIGHT));
		inputPane.add(this.latitude= new JTextField(20));
		getOKAction().mustBeInRange(this.latitude, -90.0, 90.0);
		fields.add(new Pair<FoafModel.Resource, JComponent>(getModel().createResource(Geo.NS+"lat"),this.latitude));
		
		inputPane.add(new JLabel("Longitude:",JLabel.RIGHT));
		inputPane.add(this.longitude= new JTextField(20));
		getOKAction().mustBeInRange(this.longitude, -180.0, 180.0);
		fields.add(new Pair<FoafModel.Resource, JComponent>(getModel().createResource(Geo.NS+"long"),this.longitude));
		
		inputPane.add(new JButton(new AbstractAction("Search GeoNames")
			{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				GeoNamePane ed= new GeoNamePane(SpatialThingEditor.this);
				ed.placeNameField.setText(placeName.getText());
				if(ed.showDialog()!=GeoNamePane.OK_OPTION) return;
				if(ed.geoNameSelected==null) return;
				placeName.setText(""+ed.geoNameSelected.name);
				latitude.setText(""+ed.geoNameSelected.latitude);
				longitude.setText(""+ed.geoNameSelected.longitude);
				}
			}));
		inputPane.add(new JLabel());
		}
	
	abstract FoafModel getModel();
	}



/**
 * An author in a NCBI Pubmed paper
 * @author pierre
 *
 */
class Author
	{
	String Suffix="";
	String LastName="";
	String FirstName="";
	String MiddleName="";
	String Initials="";

	static Author parse(Element root)
		{
		Author author= new Author();
		for(Node n= root.getFirstChild();n!=null;n=n.getNextSibling())
			{
			if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
			String tag= n.getNodeName();
			String content= n.getTextContent().trim();
			if(tag.equals("LastName"))
				{
				author.LastName= content;
				}
			else if(tag.equals("FirstName") || tag.equals("ForeName"))
				{
				author.FirstName= content;
				}
			else if(tag.equals("Initials"))
				{
				author.Initials= content;
				}
			else if(tag.equals("MiddleName"))
				{
				author.MiddleName= content;
				}
			else if(tag.equals("CollectiveName"))
				{
				return null;
				}
			else if(tag.equals("Suffix"))
				{
				author.Suffix= content;
				}
			else
				{
				Debug.debug("ignoring "+tag+"="+content);
				}
			}
		return author;
		}
	String toHTML()
		{
		return "<a>"+LastName+" "+FirstName+"</a>";
		}
	@Override
	public String toString() {
		return ""+LastName+" "+FirstName;
		}
	}

/**
 * A paper from PUBMED
 * @author pierre
 *
 */
class Paper
	{
	Vector<Author> authors= new Vector<Author>(10);
	HashSet<String> meshes= new HashSet<String>();
	String PMID=null;
	String ArticleTitle=null;
	String Volume=null;
	String Issue=null;
	String PubDate=null;
	String MedlinePgn=null;
	String JournalTitle=null;
	String DOI=null;
	
	private Paper()
		{
		
		}
	
	public String toHTML()
		{
		StringBuilder b= new StringBuilder("<div>");
		if(ArticleTitle!=null) b.append("<h3>"+ArticleTitle+"</h3>");
		b.append("<h4>");
		for(int i=0;i< authors.size();++i)
			{
			if(i!=0 && i+1!=authors.size()) b.append(",");
			if(i!=0 && i+1==authors.size()) b.append(" and ");
			b.append(authors.elementAt(i).toHTML());
			}
		b.append("</h4>");
		
		
		b.append("<p>");
		if(JournalTitle!=null) b.append("<b>"+JournalTitle+"</b>. ");
		if(PubDate!=null) b.append(" <i>"+PubDate+"</i>. ");
		if(Volume!=null) b.append(" ("+Volume+") ");
		if(Issue!=null) b.append(" "+Issue+", ");
		if(MedlinePgn!=null) b.append(" pp."+MedlinePgn+". ");
		b.append("</p>");
		b.append("<i>PMID.</i>"+PMID+"<br>");
		if(DOI!=null) b.append("<i>DOI.</i>"+DOI+"<br>");
		b.append("</div>");
		return b.toString();
		}
	
	static Paper parse(Element root)
		{
		Paper paper= new Paper();
		parse(paper,root,0);
		return paper;
		}
	
	static private void parse(Paper paper,Element node,int depth)
		{
		if(node==null) return;
		String name=node.getNodeName();
		if(name.equals("PMID")) { paper.PMID= node.getTextContent().trim();}
		else if(name.equals("Volume")) { paper.Volume= node.getTextContent().trim();}
		else if(name.equals("Issue")) { paper.Issue= node.getTextContent().trim();}
		else if(name.equals("MedlinePgn")) { paper.MedlinePgn= node.getTextContent().trim();}
		else if(name.equals("Title")) { paper.JournalTitle= node.getTextContent().trim();}
		else if(node.getParentNode()!=null &&
				node.getParentNode().getNodeName().equals("PubDate"))
			{
			if(paper.PubDate==null) paper.PubDate="";
			if(paper.PubDate.length()>0) paper.PubDate+="-";
			paper.PubDate+=node.getTextContent().trim();
			}
		else if(name.equals("ArticleTitle")) { paper.ArticleTitle= node.getTextContent().trim();}
		else if(name.equals("Author"))
			{
			Author author= Author.parse(node);
			if(author!=null && author.LastName!=null) paper.authors.addElement(author);
			}
		else if(name.equals("QualifierName"))
			{
			paper.meshes.add(node.getTextContent());
			}
		else if(name.equals("ArticleId"))
			{
			String doi= node.getAttribute("IdType");
			if("doi".equals(doi))
				{
				paper.DOI=node.getTextContent().trim();
				}
			}
		
			{
			for(int i=0;i< depth;++i) System.err.print("  ");
			System.err.print(name);
			for(int i=0;i< node.getAttributes().getLength();++i)
				{
				System.err.print(" "+node.getAttributes().item(i).getNodeName()+"="+node.getAttributes().item(i).getNodeValue()+" ");
				}
			if(node.getFirstChild()!=null && node.getFirstChild().getNodeType()==Node.TEXT_NODE)
				{
				System.err.print(" "+node.getFirstChild().getTextContent());
				}
			System.err.println();
			}
		
		
		for(Node n= node.getFirstChild();n!=null;n=n.getNextSibling())
			{
			if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
			parse(paper,Element.class.cast(n),depth+1);
			}
		}
	
	public static String getURI(String pmid)
		{
		return "http://view.ncbi.nlm.nih.gov/pubmed/"+pmid;
		}
	
	public String getURI()
		{
		return getURI(this.PMID);
		}
	
	}



abstract class PaperEditor extends SimpleDialog
	{
	private static final long serialVersionUID = 1L;
	private Vector<Pair<JComboBox,Author>> joinAuthor2Instance;
	private Vector<Pair<JCheckBox,String>> meshes;
	private Paper paper;
	
	abstract FoafModel getModel();
	
	PaperEditor(Component c,Paper paper) throws SQLException
		{
		super(c,"Pubmed PMID."+paper.PMID);
		Debug.debug();
		this.paper=paper;
		this.joinAuthor2Instance= new Vector<Pair<JComboBox,Author>>(paper.authors.size(),1); 
		this.meshes= new Vector<Pair<JCheckBox,String>>(paper.meshes.size());
		int predWidth= (int)(Toolkit.getDefaultToolkit().getScreenSize().width*0.75);
		JPanel pane= new JPanel(new BorderLayout(5,5));
		getContentPane().add(pane);
		JPanel top = new JPanel(new BorderLayout(5,5));
		pane.add(top,BorderLayout.NORTH);
		JEditorPane editor=new JEditorPane("text/html",
				"<html><body width='"+predWidth+"'>"+paper.toHTML()+"</body></html>");
		editor.setEditable(false);
		top.add(new JScrollPane(editor));
		top.add(new JSeparator(JSeparator.HORIZONTAL),BorderLayout.SOUTH);
		
		JPanel pane2= new JPanel(new GridLayout(1,0,10,10));
		pane.add(pane2);
		
		JPanel pane3= new JPanel(new GridLayout(0,2,3,3));
		pane3.setBorder(new TitledBorder("Authors"));
		pane2.add(new JScrollPane(pane3));
		for(Author author:paper.authors)
			{
			JLabel label= new JLabel(author.toString(),JLabel.RIGHT);
			pane3.add(label);
			JComboBox cbox= createCombo(author);
			pane3.add(cbox);
			}
		
		pane3= new JPanel(new GridLayout(0,3,3,3));
		pane3.setBorder(new TitledBorder("Mesh Terms"));
		pane2.add(new JScrollPane(pane3));
		for(String mesh:paper.meshes)
			{
			JCheckBox cb= new JCheckBox(mesh,false);
			this.meshes.add(new Pair<JCheckBox,String>(cb,mesh));
			pane3.add(cb);
			}
		}
	private JComboBox createCombo(Author author) throws SQLException
		{
		DefaultComboBoxModel m= new DefaultComboBoxModel();
		
		int selectedIndex=-1;
		m.addElement("-- Ignore --");
		m.addElement("Create New foaf:Person");
		m.addElement("As Literal");
		DerbyModel.Resource FOAF_PERSON= getModel().createResource(FOAF.NS+"Person");
		DerbyModel.Resource NCBI_AUTHOR= getModel().createResource(NCBI.NS+NCBI.Author);
		DerbyModel.Resource isNCBIAuthor= getModel().createResource(NCBI.NS+NCBI.IsNCBIAuthor);
		DerbyModel.Resource lastNameURI= getModel().createResource(NCBI.NS+NCBI.lastName);
		
		

		CloseableIterator<DerbyModel.Statement> j= getModel().listStatements(null, getModel().RDF_TYPE, FOAF_PERSON);
		while(j.hasNext())
			{
			DerbyModel.Statement i= j.next();
			m.addElement(i);
			
			CloseableIterator<DerbyModel.Statement> iter = getModel().listStatements(i.getSubject(), isNCBIAuthor, null);
			while( selectedIndex==-1 && iter.hasNext())
				{
				DerbyModel.Statement p= iter.next();
				if(!p.getPredicate().equals(isNCBIAuthor) ) continue;
				if(!p.getValue().isResource() ) continue;
				if(!p.getValue().asResource().hasProperty(getModel().RDF_TYPE,NCBI_AUTHOR)) continue;
			
				String name= p.getValue().asResource().getString(lastNameURI);
				if(name!=null && author.LastName!=null && name.equalsIgnoreCase(author.LastName))
					{
					selectedIndex=m.getSize()-1;
					}
				}
			iter.close();
			}
		j.close();
		
		JComboBox cbox= new JComboBox(m);
		cbox.setName(author.toString());
		cbox.setSelectedIndex(selectedIndex);
		getOKAction().mustBeSelected(cbox);
		joinAuthor2Instance.add(new Pair<JComboBox,Author>(cbox,author));
		return cbox;
		}
	
	
	public void create() throws SQLException
		{
		if(getModel().containsSubject(getModel().createResource(paper.getURI()))) return;
		DerbyModel.Resource instance= getModel().createResource(paper.getURI());
		instance.addProperty(getModel().RDF_TYPE, getModel().createResource(FOAF.NS+"Document"));
		
		_addpredicate(instance,NCBI.NS,"pmid",paper.PMID);
		_addpredicate(instance,DC.NS,"title",paper.ArticleTitle);
		_addpredicate(instance,NCBI.NS,"issue",paper.Issue);
		_addpredicate(instance,NCBI.NS,"volume",paper.Volume);
		_addpredicate(instance,NCBI.NS,"medlinePgn",paper.MedlinePgn);
		_addpredicate(instance,DC.NS,"date",paper.PubDate);
		_addpredicate(instance,NCBI.NS,"journalTitle",paper.JournalTitle);

		
		if(paper.DOI!=null)
			{
			try {
				instance.addProperty(
						getModel().createResource(NCBI.NS+"doi"),
						getModel().createResource("http://dx.doi.org/"+paper.DOI)
						);
			} catch (Exception e) {
				Debug.debug(e);
			}
			}
		
		for(Pair<JCheckBox,String> p:this.meshes)
			{
			if(!p.first().isSelected()) continue;
			_addpredicate(instance,DC.NS,"subject",p.second());
			}
		
		for(Pair<JComboBox,Author> p:this.joinAuthor2Instance)
			{
			DefaultComboBoxModel m= DefaultComboBoxModel.class.cast(p.first().getModel());
			int index= p.first().getSelectedIndex();
			if(index==-1) continue;
			else if(index==0) continue;//ignore
			else if(index==1)//new author
				{
				Resource author= createAuthor(p.second());
				if(author!=null)
					{
					linkAuthorAndPaper(instance,author);
					}
				}
			else if(index==2)//literal
				{
				_addpredicate(instance,FOAF.NS,"maker",p.second().toString());
				}
			else// 
				{
				Resource author=Resource.class.cast(m.getElementAt(index));
				if(author!=null)
					{
					linkAuthorAndPaper(instance,author);
					}
				}
			}
		
		
		}
	
	private void linkAuthorAndPaper(DerbyModel.Resource paper,DerbyModel.Resource author) throws SQLException
		{
		getModel().addStatement(
				paper,
				getModel().createResource(FOAF.NS+"maker"),
				author
				);
		
		getModel().addStatement(
				author,
				getModel().createResource(FOAF.NS+"made"),
				paper
				);
		}
	
	private DerbyModel.Resource createAuthor(Author o) throws SQLException
		{
		if(o.FirstName==null) o.FirstName="";

		DerbyModel.Resource author=getModel().createResource((o.FirstName+o.LastName).replaceAll("[^a-zA-Z]", ""));
		if(getModel().containsSubject(author))
			{
			author =getModel().createResource();
			}
		getModel().addStatement(
				author,
				getModel().createResource(RDF.NS+"type"),
				getModel().createResource(FOAF.NS+"Person")
				);
		
		_addpredicate(author,FOAF.NS,"name",(o.LastName+" "+o.FirstName).trim());
		_addpredicate(author,FOAF.NS,"firstName",o.FirstName);
		_addpredicate(author,FOAF.NS,"family_name",o.LastName);
		
		DerbyModel.Resource ncbiName= createNCBIAuthor(o);
		if(ncbiName!=null)
			{
			getModel().addStatement(
					author,
					getModel().createResource(NCBI.NS+NCBI.IsNCBIAuthor),
					ncbiName
					);
			}
		return author;
		}
	
	private DerbyModel.Resource createNCBIAuthor(Author o) throws SQLException
		{
		DerbyModel.Resource author= getModel().createResource();
		getModel().addStatement(
				author,
				getModel().createResource(RDF.NS+"type"),
				getModel().createResource(NCBI.NS+"Author")
				);
		
		_addpredicate(author,NCBI.NS,NCBI.firstName,o.FirstName);
		_addpredicate(author,NCBI.NS,NCBI.lastName,o.LastName);
		_addpredicate(author,NCBI.NS,"initials",o.Initials);
		_addpredicate(author,NCBI.NS,"middleName",o.MiddleName);
		_addpredicate(author,NCBI.NS,"suffix",o.Suffix);
		return author;
		}
	
	private void _addpredicate(DerbyModel.Resource instance,String ns,String local,String value) throws SQLException
		{
		if(value==null || value.trim().length()==0 ) return;
		if(value.length()> getModel().getLiteralMaxLength()) value=value.substring(0,getModel().getLiteralMaxLength());
		getModel().addStatement(
			instance,
			getModel().createResource(ns+local),
			getModel().createLiteral(value.trim())
			);
		}
}

/**
 * FoafModel
 * @author lindenb
 *
 */
class FoafModel extends DerbyModel
	{
	public final Resource RDF_TYPE= createResource(RDF.NS+"type");
	
	public FoafModel(File file) throws SQLException
		{
		super(file);
		getPrefixMapping().setNsPrefix("ncbi", NCBI.NS);
		getPrefixMapping().setNsPrefix("geo", Geo.NS);
		}
	}

/**
 * NCBI
 */
class NCBI extends Namespace
	{
	public static final String NS="http://www.ncbi.nlm.nih.gov/rdf/";
	public static final String IsNCBIAuthor="isNCBIAuthor";
	public static final String lastName="lastName";
	public static final String firstName="firstName";
	public static final String Author="Author";
	}


class StmtWrapper extends XObject
	implements Comparable<StmtWrapper>
	{
	private DerbyModel.Statement stmt;
	private DerbyModel.Resource rdfType;
	private DerbyModel.Literal title;
	StmtWrapper(DerbyModel.Statement stmt) throws SQLException
		{
		this.stmt= stmt;
		this.rdfType= this.stmt.getSubject().getPropertyAsResource(stmt.getModel().createResource(RDF.NS,"type"));
		this.title = this.stmt.getSubject().getPropertyAsLiteral(stmt.getModel().createResource(FOAF.NS,"name"));
		if(this.title==null || this.title.getString().trim().length()==0)
			{
			this.title= this.stmt.getSubject().getPropertyAsLiteral(stmt.getModel().createResource(DC.NS,"title"));
			}
		}
	
	public DerbyModel.Resource getType() {
		return rdfType;
		}
	
	
	public DerbyModel.Literal getTitle() {
		return title;
		}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(obj==null || (obj instanceof StmtWrapper)) return false;
		return getStatement().equals(StmtWrapper.class.cast(obj).getStatement());
		}
	
	public DerbyModel.Statement getStatement() {
		return stmt;
		}
	
	
	
	public DerbyModel getModel() {
		return getStatement().getModel();
	}

	public Resource getPredicate() {
		return getStatement().getPredicate();
	}

	public Resource getSubject() {
		return getStatement().getSubject();
	}

	public RDFNode getValue() {
		return getStatement().getValue();
	}

	@Override
	public int hashCode() {
		return getStatement().hashCode();
		}
	
	@Override
	public String toString() {
		return getStatement().toString();
		}
	
	@Override
	public int compareTo(StmtWrapper o) {
		return getStatement().compareTo(o.getStatement());
		}
	
	}


class RDFNodeRenderer extends DefaultTableCellRenderer
	{
	private static final long serialVersionUID = 1L;
	RDFNodeRenderer()
		{
		
		}
	
	@Override
	public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column)
		{
		
		if(DerbyModel.RDFNode.class.isInstance(value))
			{
			DerbyModel.RDFNode rdfNode= DerbyModel.RDFNode.class.cast(value);
			if(value!=null)
				{
				if(rdfNode.isLiteral())
					{
					value = rdfNode.asLiteral().getString();
					setForeground(Color.GRAY);
					}
				else
					{
					value = rdfNode.asResource().getShortName();
					setForeground(Color.BLUE);
					}
				}
			}
		
		Component c= super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);
		return c;
		}
	}

/**
 * SciFOAFApplication
 * @author pierre
 *
 */
public class SciFOAFApplication extends JFrame {
	private static final long serialVersionUID = 1L;
	private JDesktopPane desktopPane;
	private JMenu windowsMenu;
	private Observed<FoafModel> derbyModel= new Observed<FoafModel>();
	private AbstractAction menuNewAction;
	private AbstractAction menuOpenAction;
	private AbstractAction menuCloseAction;

	
	/**
	 * any internal frame in the this.desktopPane
	 * @author pierre
	 *
	 */
	private class MyInternalFrame extends JInternalFrame
		{
		private static final long serialVersionUID = 1L;

		MyInternalFrame(String title)
			{
			super(title,true,true,true,true);
			setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
			addInternalFrameListener(new InternalFrameAdapter()
				{
				@Override
				public void internalFrameOpened(InternalFrameEvent e) {
					updateMenuWindow();
					}
				@Override
				public void internalFrameClosed(InternalFrameEvent e) {
					updateMenuWindow();
					}
				});
			if(desktopPane!=null)
				{
				Random rand= new Random();
				Dimension dim= desktopPane.getSize();
				setBounds(rand.nextInt(20),rand.nextInt(20),
						dim.width-40,
						dim.height-40);
				}
			}
		}
	
	private class StatementTableModel extends GenericTableModel<StmtWrapper>
		{
		private static final long serialVersionUID = 1L;
		
		@Override
		public Class<?> getColumnClass(int columnIndex)
			{
			switch(columnIndex)
				{
				case 0: return DerbyModel.Resource.class;
				case 1: return DerbyModel.Resource.class;
				case 2: return DerbyModel.RDFNode.class;
				case 3: return DerbyModel.Resource.class;
				case 4: return DerbyModel.Literal.class;
				default:return null;
				}
			}
		
		@Override
		public String getColumnName(int columnIndex)
			{
			switch(columnIndex)
				{
				case 0: return "Subject";
				case 1: return "Predicate";
				case 2: return "Value";
				case 3: return "rdf:type";
				case 4: return "Title";
				default:return null;
				}
			}
		
		@Override
		public int getColumnCount() {
			return 5;
			}
		@Override
		public Object getValueOf(StmtWrapper object, int columnIndex) {
			switch(columnIndex)
				{
				case 0: return object.getSubject();
				case 1: return object.getPredicate();
				case 2: return object.getValue();
				case 3: return object.getType();
				case 4: return object.getTitle();
				default:return null;
				}
			}
		}
	
	/**
	 * 
	 * @author lindenb
	 *
	 */
	private class StatementTable extends JTable
		{
		private static final long serialVersionUID = 1L;
		StatementTable()
			{
			super(new StatementTableModel());
			setShowVerticalLines(false);
			setRowHeight(22);
			setFont(new Font("Dialog",Font.BOLD,18));
			
			RDFNodeRenderer cr= new RDFNodeRenderer();
			for(int i=0;i< getColumnModel().getColumnCount();++i)
				{
				getColumnModel().getColumn(i).setCellRenderer(cr);
				}
			setToolTipText("");
			}
		
		@Override
		public String getToolTipText(MouseEvent event) {
			int r= this.rowAtPoint(event.getPoint());
			if(r==-1) return null;
			if((r=convertRowIndexToModel(r))==-1) return null;
			int c= this.columnAtPoint(event.getPoint());
			if(c==-1) return null;
			if((c=convertColumnIndexToModel(c))==-1) return null;
			Object o= getModel().getValueAt(r, c);
			return o==null?null:o.toString();
			}
		
		public StatementTableModel getStmtTable()
			{
			return StatementTableModel.class.cast(getModel());
			}
		
		public DerbyModel.Statement getStatementAt(int index)
			{
			if(index==-1) return null;
			index= convertRowIndexToModel(index);
			if(index==-1) return null;
			return getStmtTable().elementAt(index).getStatement();
			}
		
		public Vector<DerbyModel.Statement> getSelectedStatements()
			{
			int indexes[]=getSelectedRows();
			 Vector<Statement> sel= new  Vector<Statement>(indexes.length);
			 for(int i=0;i< indexes.length;++i)
			 	{
				 DerbyModel.Statement stmt= getStatementAt(indexes[i]);
				if(stmt==null) continue;
				sel.addElement(stmt);
			 	}
			return sel; 
			}
		
		}
	
	
	private static String _shortName(RDFNode r)
	{
	if(r==null) return "*";
	String s=r.isLiteral()?r.asLiteral().getString():r.asResource().getShortName();
	s=(s.length()>20?s.substring(0,20)+"...":s);
	return r.isLiteral()?"\""+s+"\"":"<"+s+">";
	}
	
	/**
	 * 
	 * @author pierre
	 *
	 */
	private class StatementsFrame extends MyInternalFrame
		{
		private static final long serialVersionUID = 1L;
		
		class SuggestPredicateAction extends ObjectAction<String>
			{
			private static final long serialVersionUID = 1L;
			SuggestPredicateAction(String ns,String prefix,String local)
				{
				super(ns+local,prefix+":"+local);
				}
			@Override
			public void actionPerformed(ActionEvent e)
				{
				if(predicateTextField==null) return;
				predicateTextField.setText(getObject());
				predicateTextField.setCaretPosition(0);
				}
			}
		
		class SuggestValueAction extends ObjectAction<String>
			{
			private static final long serialVersionUID = 1L;
			SuggestValueAction(String ns,String prefix,String local)
				{
				super(ns+local,prefix+":"+local);
				}
			@Override
			public void actionPerformed(ActionEvent e)
				{
				if(valueTextField==null || valueTypeCombo.getSelectedIndex()!=0) return;
				valueTextField.setText(getObject());
				valueTextField.setCaretPosition(0);
				}
			}
		
		
		
		private class DeployStatementAction extends ObjectAction<DerbyModel.RDFNode[]>
			{
			private static final long serialVersionUID = 1L;
			DeployStatementAction(
					DerbyModel.Resource S,
					DerbyModel.Resource P,
					DerbyModel.RDFNode V)
				{
				super(new DerbyModel.RDFNode[]{S,P,V},
						"("+
						_shortName(S)+","+
						_shortName(P)+","+
						_shortName(V)+
						")");
				}
		
		
			private boolean _same(RDFNode n1,RDFNode n2)
				{
				if(n1==null && n2==null) return true;
				if(n1==null && n2!=null) return false;
				if(n1!=null && n2==null) return false;
				return n1.equals(n2);
				}
			
			@Override
			public void actionPerformed(ActionEvent e)
				{
				for(JInternalFrame frames: desktopPane.getAllFrames())
					{
					if(!(frames instanceof StatementsFrame)) continue;
					StatementsFrame cp=StatementsFrame.class.cast(frames);
					if( _same(getObject()[0],cp.subject) &&
						_same(getObject()[1],cp.predicate) &&
						_same(getObject()[2],cp.value)
						)
						{
						cp.moveToFront();
						return;
						}
					
					
					}
				StatementsFrame f= new StatementsFrame(
					DerbyModel.Resource.class.cast(getObject()[0]),
					DerbyModel.Resource.class.cast(getObject()[1]),
					getObject()[2]
					);
				f.setClosable(true);
				f.setTitle(this.getValue(AbstractAction.NAME).toString());
				SciFOAFApplication.this.desktopPane.add(f);
				
				f.setVisible(true);
				}
			}
		
		
		ConstrainedAction<StatementTable> removeStatementAction;
		DerbyModel.Resource subject;
		DerbyModel.Resource predicate;
		DerbyModel.RDFNode value;
		protected StatementTable table;
		private AbstractAction goPrevPageAction;
		private AbstractAction goNextPageAction;
		JTextField subjectTextField;
		JTextField predicateTextField;
		JTextField valueTextField;
		JComboBox valueTypeCombo;
		AbstractAction setAnonymouseNodeAction;
		AbstractAction chooseSubjectAction;
		AbstractAction choosePredicateAction;
		AbstractAction chooseValueAsResourceAction;
		AbstractAction createStatementAction;
		private JMenu exploreMenu;
		private ConstrainedAction<StatementsFrame> reloadAction;
		private JLabel pagerPageLabel;
		private JSpinner pagerLimitSpinner;
		private JSpinner pagerPageStart;
		private JTextField pagerRegexFilter;
		
		StatementsFrame()
			{
			this(null,null,null);	
			}
		
		StatementsFrame(DerbyModel.Resource subject,DerbyModel.Resource predicate,DerbyModel.RDFNode value)
			{
			super("Statements");
			this.subject=subject;
			this.predicate=predicate;
			this.value=value;
			setClosable(true);
			JMenuBar bar= new JMenuBar();
			setJMenuBar(bar);
			JMenu menu= new JMenu("RDF");
			bar.add(menu);
			this.exploreMenu= new JMenu("Browse");
			bar.add(exploreMenu);
			JMenu toolMenu= new JMenu("Tool");
			bar.add(toolMenu);
			if(subject==null && predicate==null && subject==null)
				{
				toolMenu.add(new JMenuItem(new AbstractAction("New Article")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						SciFOAFApplication.this.doMenuNewPaper(StatementsFrame.this);
						
						}
					}));
				
				toolMenu.add(new JMenuItem(new AbstractAction("New Image")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						
						
						}
					}));
				}	
			
			
			
			
			addInternalFrameListener(new InternalFrameAdapter()
				{
				@Override
				public void internalFrameOpened(InternalFrameEvent e) {
					StatementsFrame.this.fireRDFModelUpdated();
					}
				});
			
			
			JPanel contentPane= new JPanel(new BorderLayout(5,5));
			contentPane.setBorder(new EmptyBorder(5,5,5,5));
			setContentPane(contentPane);
			contentPane.add(new JScrollPane(this.table= new StatementTable()));
			this.table.addMouseListener(new MouseAdapter()
				{
				@Override
				public void mousePressed(MouseEvent e) {
					if(!e.isPopupTrigger()) return;
					int i=table.rowAtPoint(e.getPoint());
					JPopupMenu menu= new JPopupMenu();
					DerbyModel.Statement stmt= table.getStatementAt(i);
					if(stmt==null) return;
					fillMenuOnSelect(menu,stmt);
				

					menu.show(e.getComponent(), e.getX(), e.getY());
					}
				});
			
			
			
			JPanel pane1= new JPanel(new GridLayout(1,0,0,0));
			contentPane.add(pane1,BorderLayout.NORTH);
			
			JPanel pane2= new JPanel(new InputLayout());
			pane1.add(pane2);
			pane2.add(new JLabel("Subject:",JLabel.RIGHT));
			pane2.add(new JLabel(this.subject==null?"*":this.subject.getShortName()));
			pane2.add(new JLabel("Predicate:",JLabel.RIGHT));
			pane2.add(new JLabel(this.predicate==null?"*":this.predicate.getShortName()));
			pane2.add(new JLabel("Value:",JLabel.RIGHT));
			pane2.add(new JLabel(this.value==null?"*":this.value.isResource()?value.asResource().getURI():value.asLiteral().getString()));
			SwingUtils.setFontSize(pane2,9);
			pane2.setBorder(new LineBorder(Color.GRAY,1));
			
			pane2= new JPanel(new FlowLayout(FlowLayout.CENTER));
			pane1.add(pane2);
			
			
				{
				JPanel pager= new JPanel(new BorderLayout());
				pane2.add(pager);
				
				this.goPrevPageAction= new AbstractAction("Prev")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						int pageToReach = Number.class.cast(pagerPageStart.getValue()).intValue();
						if(pageToReach==1)
							{
							Toolkit.getDefaultToolkit().beep();
							return;
							}
						pagerPageStart.setValue(pageToReach-1);
						fireRDFModelUpdated();
						}
					};
				pager.add(new JButton(this.goPrevPageAction),BorderLayout.WEST);
				
				
				
				this.goNextPageAction= new AbstractAction("Next")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						int pageToReach = Number.class.cast(pagerPageStart.getValue()).intValue();
						pagerPageStart.setValue(pageToReach+1);
						fireRDFModelUpdated();
						}
					};
				pager.add(new JButton(this.goNextPageAction),BorderLayout.EAST);
				
				
				pager.add(this.pagerPageLabel=new JLabel("Page 1",JLabel.CENTER),BorderLayout.NORTH);
				
				JPanel pane3= new JPanel(new FlowLayout(FlowLayout.CENTER));
				pager.add(pane3,BorderLayout.SOUTH);
				pane3.add(new JButton(this.reloadAction=new ConstrainedAction<StatementsFrame>(this,"Reload")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						fireRDFModelUpdated();
						}
					}));
				
				pane3= new JPanel(new GridLayout(0,1));
				pager.add(pane3,BorderLayout.CENTER);
				
				JPanel pane4= new JPanel(new InputLayout());
				pane3.add(pane4);
				pane4.add(new JLabel("Page Start:",JLabel.RIGHT));
				pane4.add(this.pagerPageStart=new JSpinner(new SpinnerNumberModel(1,1,Integer.MAX_VALUE,1)));
				pane4.add(new JLabel("Limit:",JLabel.RIGHT));
				pane4.add(this.pagerLimitSpinner=new JSpinner(new SpinnerNumberModel(100,1,Integer.MAX_VALUE,1)));
				pane4.add(new JLabel("Filter:",JLabel.RIGHT));
				pane4.add(this.pagerRegexFilter=new JTextField(".*",15));
				
				this.reloadAction.mustBeARegexPattern(this.pagerRegexFilter);
				SwingUtils.setFontSize(pager,9);
				}
			
			
			
			
			
			this.removeStatementAction= new ConstrainedAction<StatementTable>(this.table,"Remove")
				{
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					
					Vector<DerbyModel.Statement> toremove= table.getSelectedStatements();
					if(toremove.isEmpty()) return;
					
					try {getRDFModel().remove(toremove);
					} catch (SQLException er) {
						ThrowablePane.show(table, er);}
					
					fireRDFModelUpdated();
					}
				};
			this.removeStatementAction.setEnabled(false);
			this.removeStatementAction.mustBeSelected(this.table);
			
			menu.add(new JMenuItem(removeStatementAction));
			
			
			
			this.createStatementAction=new AbstractAction("Create Statement")
				{
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					doMenuCreateStatement();
					}
				};
			this.createStatementAction.setEnabled(false);
			
			JPanel bottom= new JPanel(new BorderLayout());
			Font smallFont= new Font("Dialog",Font.PLAIN,10);
			
			this.add(bottom,BorderLayout.SOUTH);
			JPanel pane3= new JPanel(new InputLayout());
			bottom.add(pane3,BorderLayout.CENTER);
			
			if(this.subject==null)
				{
				pane3.add(SwingUtils.withFont(new JLabel("Subject",JLabel.RIGHT),smallFont));
				JPanel pane4= new JPanel(new BorderLayout());
				pane3.add(pane4);
				pane4.add(SwingUtils.withFont(this.subjectTextField=new JTextField(20),smallFont),BorderLayout.CENTER);
				
				JPanel pane5= new JPanel(new FlowLayout(FlowLayout.LEADING));
				pane4.add(pane5,BorderLayout.EAST);
				pane5.add(SwingUtils.withFont(new JButton(setAnonymouseNodeAction=new AbstractAction("AnId")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							DerbyModel.Resource r=getRDFModel().createResource();
							subjectTextField.setText(r.getURI());
							subjectTextField.setCaretPosition(0);
							} 
						catch (SQLException err)
							{
							ThrowablePane.show(StatementsFrame.this, err);
							}
						}
					}),smallFont));
				
				pane5.add(SwingUtils.withFont(new JButton(chooseSubjectAction=new AbstractAction("Choose")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						ResourceSelector sel= new ResourceSelector(StatementsFrame.this,0);
						sel.reloadModel();
						if(sel.showDialog()!=ResourceSelector.OK_OPTION|| sel.getSelectedResource()==null) return;
						subjectTextField.setText(sel.getSelectedResource().getURI());
						subjectTextField.setCaretPosition(0);
						}
					}),smallFont));
				}
			
			if(this.predicate==null)
				{
				pane3.add(SwingUtils.withFont(new JLabel("Predicate",JLabel.RIGHT),smallFont));
				JPanel pane4= new JPanel(new BorderLayout());
				pane3.add(pane4);
				pane4.add(SwingUtils.withFont(this.predicateTextField=new JTextField(20),smallFont));
				JPanel pane5= new JPanel(new FlowLayout(FlowLayout.RIGHT));
				pane4.add(pane5,BorderLayout.EAST);
				
				JButton suggestPredicate= new JButton("[...]");
				suggestPredicate.addMouseListener(new MouseAdapter()
					{
					@Override
					public void mousePressed(MouseEvent e)
						{
						if(!predicateTextField.isEnabled()) return;
						JPopupMenu menu= new JPopupMenu();
						makeSuggestPredicateAction(menu,RDF.NS,"rdf","type");
						makeSuggestPredicateAction(menu,DC.NS,"dc","title");
						makeSuggestPredicateAction(menu,DC.NS,"dc","date");
						makeSuggestPredicateAction(menu,FOAF.NS,"foaf","name");
						makeSuggestPredicateAction(menu,FOAF.NS,"foaf","firstname");
						makeSuggestPredicateAction(menu,FOAF.NS,"foaf","family_name");
						menu.show(e.getComponent(), e.getX(), e.getY());
						}
					});
				pane5.add(SwingUtils.withFont(suggestPredicate,smallFont));
				
				pane5.add(SwingUtils.withFont(new JButton(choosePredicateAction=new AbstractAction("Choose")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						ResourceSelector sel= new ResourceSelector(StatementsFrame.this,1);
						sel.reloadModel();
						if(sel.showDialog()!=ResourceSelector.OK_OPTION|| sel.getSelectedResource()==null) return;
						predicateTextField.setText(sel.getSelectedResource().getURI());
						predicateTextField.setCaretPosition(0);
						}
					}),smallFont));
				
				}
			
			if(this.value==null)
				{
				pane3.add(this.valueTypeCombo=new JComboBox(new String[]{"Resource","Literal"}));
				this.valueTypeCombo.setFont(smallFont);
				JPanel pane4= new JPanel(new BorderLayout());
				pane3.add(pane4);
				pane4.add(SwingUtils.withFont(this.valueTextField=new JTextField(20),smallFont));
				JPanel pane5= new JPanel(new FlowLayout(FlowLayout.RIGHT));
				pane4.add(pane5,BorderLayout.EAST);
				
				JButton suggestValue= new JButton("[...]");
				suggestValue.addMouseListener(new MouseAdapter()
					{
					@Override
					public void mousePressed(MouseEvent e)
						{
						if(!predicateTextField.isEnabled()) return;
						JPopupMenu menu= new JPopupMenu();
						makeSuggestValueAction(menu,FOAF.NS,"foaf","Person");
						makeSuggestValueAction(menu,FOAF.NS,"foaf","Group");
						makeSuggestValueAction(menu,FOAF.NS,"foaf","Image");
						menu.show(e.getComponent(), e.getX(), e.getY());
						}
					});
				pane5.add(SwingUtils.withFont(suggestValue,smallFont));
				
				pane5.add(SwingUtils.withFont(new JButton(chooseValueAsResourceAction=new AbstractAction("Choose")
					{
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e)
						{
						ResourceSelector sel= new ResourceSelector(StatementsFrame.this,0);
						sel.reloadModel();
						if(sel.showDialog()!=ResourceSelector.OK_OPTION|| sel.getSelectedResource()==null) return;
						valueTextField.setText(sel.getSelectedResource().getURI());
						valueTextField.setCaretPosition(0);
						}
					}),smallFont),BorderLayout.EAST);
				
				
				this.valueTypeCombo.addActionListener(new ActionListener()
					{
					@Override
					public void actionPerformed(ActionEvent e) {
						chooseValueAsResourceAction.setEnabled(valueTypeCombo.getSelectedIndex()==0);
						updateControls();	
					}
					});
				
				
				this.valueTextField.getDocument().addDocumentListener(new DocumentAdapter()
					{
					@Override
					public void documentChanged(DocumentEvent e) {
						updateControls();
						}
					});
				}
			
			
		if(this.predicateTextField!=null)
			{
			this.predicateTextField.getDocument().addDocumentListener(new DocumentAdapter()
				{
				@Override
				public void documentChanged(DocumentEvent e) {
					updateControls();
					}
				});
			}
		
			if(this.subject==null)
				{
				this.subjectTextField.getDocument().addDocumentListener(new DocumentAdapter()
					{
					@Override
					public void documentChanged(DocumentEvent e) {
						updateControls();
						}
					});
				}
			
			JPanel pane5= new JPanel(new FlowLayout(FlowLayout.RIGHT));
			bottom.add(pane5,BorderLayout.SOUTH);
			pane5.add(new JButton(createStatementAction));
			menu.add(new JMenuItem(createStatementAction));
			
			
			this.table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
				{
				@Override
				public void valueChanged(ListSelectionEvent e) {
					exploreMenu.removeAll();
					if(table.getSelectedRowCount()==1)
						{
						for(DerbyModel.Statement stmt:table.getSelectedStatements())
							{
							fillMenuOnSelect(exploreMenu,stmt);
							if(subjectTextField!=null)
								{
								subjectTextField.setText(stmt.getSubject().getURI());
								subjectTextField.setCaretPosition(0);
								}
							if(predicateTextField!=null)
								{
								predicateTextField.setText(stmt.getPredicate().getURI());
								predicateTextField.setCaretPosition(0);
								}
							if(valueTextField!=null)
								{
								valueTextField.setText(stmt.getValue().isLiteral()?
									stmt.getValue().asLiteral().getString():
									stmt.getValue().asResource().getURI()
									);
								valueTypeCombo.setSelectedIndex(stmt.getValue().isLiteral()?1:0);
								valueTextField.setCaretPosition(0);
								}
							break;
							}
						}
					}
				});
			
			}
		
		private void makeSuggestPredicateAction(JPopupMenu menu,String ns,String prefix,String local)
			{
			menu.add(new JMenuItem(new SuggestPredicateAction(ns,prefix,local)));
			}
		
		private void makeSuggestValueAction(JPopupMenu menu,String ns,String prefix,String local)
			{
			menu.add(new JMenuItem(new SuggestValueAction(ns,prefix,local)));
			}
		
		private void doMenuCreateStatement()
			{
			if(!isActionCreateShouldBeEnabled()) return;
			try
				{
				DerbyModel.Resource S= 
					(this.subject!=null?this.subject:getRDFModel().createResource(this.subjectTextField.getText().trim()));
				
				DerbyModel.Resource P=
					(this.predicate!=null?this.predicate:getRDFModel().createResource(this.predicateTextField.getText().trim()));
				DerbyModel.RDFNode V=this.value;
				
				if(this.value==null)
					{
					switch(this.valueTypeCombo.getSelectedIndex())
						{
						case -1: return;
						case 0:
							{
							V=  getRDFModel().createResource(this.valueTextField.getText().trim());
							break;
							}
						case 1:
							{
							V=  getRDFModel().createLiteral(this.valueTextField.getText().trim());
							break;
							}
						default:return;
						}
					}
				getRDFModel().addStatement(S, P, V);
				if(this.subjectTextField!=null) this.subjectTextField.setText("");
				if(this.predicateTextField!=null) this.predicateTextField.setText("");
				if(this.valueTextField!=null) this.valueTextField.setText("");
				if(valueTypeCombo!=null) this.valueTypeCombo.setSelectedIndex(-1);
				SciFOAFApplication.this.fireRDFModelUpdated();
				}
			catch(Exception err)
				{
				ThrowablePane.show(this, err);
				}
			}
		
		
		private void fillMenuOnSelect(JComponent menu,DerbyModel.Statement stmt)
			{
			
			for(int i=0;i<2;++i)
				for(int j=0;j<2;++j)
					for(int k=0;k<2;++k)
						{
						if(i==0 && j==0 && k==0) continue;
						menu.add(new JMenuItem(new DeployStatementAction(
							i==0?null:stmt.getSubject(),
							j==0?null:stmt.getPredicate(),
							k==0?null:stmt.getValue()
							)));
						}
			if(stmt.getValue().isResource())
				{
				menu.add(new JMenuItem(new DeployStatementAction(
					stmt.getValue().asResource(),
					null,null
					)));
				}
			menu.add(new JMenuItem(new DeployStatementAction(
					null,null,
					stmt.getSubject().asResource()
					)));
			
			
			
			
			/** add HyperLink handler opening a web browser if desktop API is supported */
			 if (Desktop.isDesktopSupported())
				 {
				 menu.add(new JSeparator());
				/* if subject is URL */
				if(stmt.getSubject().isURL())
					{
					menu.add(new JMenuItem(new ObjectAction<URL>(stmt.getSubject().asURL(),"Open "+stmt.getSubject().getURI())
						{
						private static final long serialVersionUID = 1L;
						@Override
						public void actionPerformed(ActionEvent e) {
							try {
								Desktop.getDesktop().browse(getObject().toURI());
							} catch (Exception e1) {
								ThrowablePane.show(StatementsFrame.this, e1);
								}
							}
						}));
					}
				/* if value is URL */
				if(stmt.getValue().isResource() &&
					stmt.getValue().asResource().isURL())
					{
					menu.add(new JMenuItem(new ObjectAction<URL>(stmt.getValue().asResource().asURL(),"Open "+stmt.getSubject().getURI())
						{
						private static final long serialVersionUID = 1L;
						@Override
						public void actionPerformed(ActionEvent e) {
							try {
								Desktop.getDesktop().browse(getObject().toURI());
							} catch (Exception e1) {
								ThrowablePane.show(StatementsFrame.this, e1);
								}
							}
						}));
					}
				 }
			 else
			 	{
				Debug.debug("Desktop API not supported"); 
			 	}
			
		    menu.add(new JSeparator());
			try
				{
				if(stmt.getSubject().hasProperty(
					getRDFModel().RDF_TYPE,
					getRDFModel().createResource(FOAF.NS, "Person")))
					{
					menu.add(new JMenuItem(new ObjectAction<DerbyModel.Resource>(stmt.getSubject(),"Add NCBI Name")
							{
							private static final long serialVersionUID = 1L;
							@Override
							public void actionPerformed(ActionEvent e) {
								//
								}
							}));
					
					
					}
					
				
				
				if(
				   stmt.getSubject().hasProperty( getRDFModel().RDF_TYPE, getRDFModel().createResource(FOAF.NS, "Person"))
				   )
					{
					menu.add(new JMenuItem(new ObjectAction<DerbyModel.Resource>(stmt.getSubject(),"Add Location")
							{
							private static final long serialVersionUID = 1L;
							@Override
							public void actionPerformed(ActionEvent e) {
								SpatialThingEditor ed= new SpatialThingEditor(StatementsFrame.this)
									{
									private static final long serialVersionUID = 1L;
									@Override
									FoafModel getModel() {
										return SciFOAFApplication.this.getRDFModel();
										}
									};
								if(ed.showDialog()!=SpatialThingEditor.OK_OPTION) return;
									}
								
							}));
					
					menu.add(new JMenuItem(new ObjectAction<DerbyModel.Resource>(stmt.getSubject(),"Add NCBI Name")
							{
							private static final long serialVersionUID = 1L;
							@Override
							public void actionPerformed(ActionEvent e) {
								//
								}
							}));
					}
				
				}
			catch(SQLException err)
				{
				ThrowablePane.show(StatementsFrame.this, err);
				}
			
			
			
			
			}
		
		
		private boolean isActionCreateShouldBeEnabled()
			{
			if(this.subject==null)
				{
				try {
					String s= this.subjectTextField.getText().trim();
					if(s.length()==0 || s.length()>= getRDFModel().getResourceMaxLength()) return false;
					new URI(s.trim());
					} catch (URISyntaxException e) {
					return false;
					}
				}
			
			if(this.predicate==null)
				{
				try {
					String s= this.predicateTextField.getText().trim();
					if(s.length()==0 || s.length()>= getRDFModel().getResourceMaxLength()) return false;
					new URL(s.trim());
					} catch (MalformedURLException e) {
					return false;
					}
				}
			
			if(this.value==null)
				{
				String s= this.valueTextField.getText().trim();
				switch(this.valueTypeCombo.getSelectedIndex())
					{
					case -1: return false;
					case 0:
						{
						try {
							if(s.length()==0 || s.length()>= getRDFModel().getResourceMaxLength()) return false;
							new URI(s);
							} catch (URISyntaxException e) {
							return false;
							}
						break;
						}
					case 1: return s.length()>0  &&   s.length()< getRDFModel().getLiteralMaxLength();
					}
				}
			return true;
			}
		
		
		private void updateControls()
			{
			createStatementAction.setEnabled(isActionCreateShouldBeEnabled());
			}
		
		
		public void fireRDFModelUpdated()
			{
			Cursor oldCursor= this.getCursor();
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			Vector<StmtWrapper> stmts= new Vector<StmtWrapper>();
			HashSet<DerbyModel.Statement> wereSeleted= new HashSet<Statement>(table.getSelectedStatements());
			HashSet<Integer> rowsToSelect= new HashSet<Integer>();
			int pageToReach = Number.class.cast(this.pagerPageStart.getValue()).intValue()-1;
			int maxStatementPerPage= Number.class.cast(this.pagerLimitSpinner.getValue()).intValue();
			Pattern pattern=null;
			
			try {
				pattern= Pattern.compile(this.pagerRegexFilter.getText(),Pattern.CASE_INSENSITIVE);
				}
			catch (PatternSyntaxException e) {
				pattern=null;
				}
			
			int currentPage=0;
			
			try
				{
				
				DerbyModel.CloseableIterator<DerbyModel.Statement> iter=
					getRDFModel().listStatements(
							this.subject,
							this.predicate,
							this.value
							);
				while(iter.hasNext() && stmts.size()< maxStatementPerPage)
					{
					DerbyModel.Statement x= iter.next();
					
					if(pattern!=null && !matches(x,pattern))
						{
						continue;
						}
					
					stmts.add(new StmtWrapper(x));

					if(wereSeleted.contains(x))
						{
						rowsToSelect.add(stmts.size()-1);
						}
					
					if(stmts.size()>= maxStatementPerPage )
						{
						if(currentPage==pageToReach) break;
						++currentPage;
						stmts.clear();
						rowsToSelect.clear();
						}
					
					}
				iter.close();
				}
			catch(SQLException err)
				{
				ThrowablePane.show(this, err);
				}
			
			if(currentPage!=pageToReach)
				{
				stmts.clear();
				rowsToSelect.clear();
				}
			
			this.table.getStmtTable().clear();
			this.table.getStmtTable().addAll(stmts);
			for(Integer n:rowsToSelect)
				{
				this.table.getSelectionModel().addSelectionInterval(n, n);
				}
			this.pagerPageLabel.setText("Page "+(pageToReach+1));
			setCursor(oldCursor);
			}
		
		private boolean matches(DerbyModel.Statement stmt,Pattern pat)
			{
			return	matches(stmt.getSubject(),pat) ||
					matches(stmt.getPredicate(),pat) ||
					matches(stmt.getValue(),pat)
					;
			}
		
		private boolean matches(DerbyModel.RDFNode x,Pattern pat)
			{
			if(x.isLiteral())
				{
				return pat.matcher(x.asLiteral().getString()).find();
				}
			return  pat.matcher(x.asResource().getURI()).find() ||
			 		pat.matcher(x.asResource().getShortName()).find()
			 		;
			}
		}
	
	/**
	 * ResourceSelector
	 * @author pierre
	 *
	 */
	private class ResourceSelector extends SimpleDialog
		{
		private static final long serialVersionUID = 1L;
		private JList list;
		private JTextField regexTextField;
		private ConstrainedAction<ResourceSelector> filterAction;
		private JSpinner spinLimit;
		private DerbyModel.Resource selected=null;
		private DerbyModel.Resource withProperty=null;
		private DerbyModel.RDFNode withValue=null;
		private int resourceType;
		
		ResourceSelector(Component owner,
				DerbyModel.Resource withProperty,
				DerbyModel.RDFNode withValue)
			{
			this(owner,0);
			this.withProperty=withProperty;
			this.withValue=withValue;
			}
		
		
		ResourceSelector(Component owner,int resourceType)
			{
			super(owner,"Resource Selector");
			this.resourceType=resourceType;
			JPanel contentPane= new JPanel(new BorderLayout());
			getContentPane().add(contentPane);
			
			Font small= new Font("Dialog",Font.PLAIN,10);
			JPanel top= new JPanel(new FlowLayout(FlowLayout.LEADING));
			contentPane.add(top,BorderLayout.NORTH);
			top.add(SwingUtils.withFont(new JLabel("Filter:",JLabel.RIGHT),small));
			top.add(this.regexTextField=new JTextField(".*",15));
			this.regexTextField.setFont(small);
			top.add(SwingUtils.withFont(new JLabel("Limit:",JLabel.RIGHT),small));
			top.add(this.spinLimit= new JSpinner(new SpinnerNumberModel(10,1,Integer.MAX_VALUE,1)));
			this.spinLimit.setFont(small);
			top.add(SwingUtils.withFont(new JButton(this.filterAction= new ConstrainedAction<ResourceSelector>(this,"Filter")
				{
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					reloadModel();
					}
				}),small));
			this.filterAction.mustBeARegexPattern(this.regexTextField);
			contentPane.add(new JScrollPane(this.list=new JList(new DefaultListModel())
				{
				private static final long serialVersionUID = 1L;

				@Override
				public String getToolTipText(MouseEvent event) {
					int i=this.locationToIndex(event.getPoint());
					if(i==-1) return null;
					return getModel().getElementAt(i).toString();
					}
				}),BorderLayout.CENTER);
			this.list.setToolTipText("");
			this.list.setFont(new Font("Dialog",Font.BOLD,18));
			this.list.setFixedCellHeight(22);
			this.list.setName("Resources List");
			this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.list.setCellRenderer(new DefaultListCellRenderer()
				{
				private static final long serialVersionUID = 1L;
				@Override
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus)
					{
					DerbyModel.Resource r=DerbyModel.Resource.class.cast(value);
					value=r.getShortName();
					Component c= super.getListCellRendererComponent(list, value, index, isSelected,
							cellHasFocus);
					return c;
					}
				});
			this.list.getSelectionModel().addListSelectionListener(new ListSelectionListener()
				{
				@Override
				public void valueChanged(ListSelectionEvent e) {
					selected=DerbyModel.Resource.class.cast(list.getSelectedValue());
					}
				});
			getOKAction().mustHaveOneRowSelected(this.list);
			}
		
				
		public DerbyModel.Resource getSelectedResource()
			{
			return this.selected;
			}
		
		public void reloadModel()
			{
			try {
				int limit= Number.class.cast(this.spinLimit.getValue()).intValue();
				Vector<DerbyModel.Resource> resources= new Vector<DerbyModel.Resource>();
				Pattern pattern= null;
				String regex = this.regexTextField.getText().trim();
				if(regex.length()>0)
					{
					try {
						pattern=Pattern.compile(this.regexTextField.getText(),Pattern.CASE_INSENSITIVE);
					} catch (PatternSyntaxException e)
						{
						Toolkit.getDefaultToolkit().beep();
						pattern=null;
						}
					}
				
				
				CloseableIterator<DerbyModel.Resource> iter;
				if(this.resourceType==0)
					{
					iter= getRDFModel().listSubjects();
					}
				else
					{
					iter = getRDFModel().listProperties();
					}
				while(iter.hasNext())
					{
					DerbyModel.Resource r= iter.next();
					
					if(pattern!=null)
						{
						if( !pattern.matcher(r.getURI()).find() &&
							!pattern.matcher(r.getShortName()).find())
							{
							continue;
							}
						}
					if(this.withProperty!=null)
						{
						DerbyModel.Statement stmt =r.getProperty(this.withProperty);
						if(stmt==null || !withValue.equals(stmt.getValue())) continue;
						}
					resources.addElement(r);
					if(resources.size()>=limit) break;
					}
				iter.close();
				DefaultListModel m = DefaultListModel.class.cast(this.list.getModel());
				m.clear();
				for(DerbyModel.Resource r:resources)
					{
					m.addElement(r);
					}
				} 
			catch (SQLException err)
				{
				}
			}
		
		}
	
	
	public SciFOAFApplication() {
		super("SciFOAF");
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		
		TrayIcon trayIcon = null;
		if(SystemTray.isSupported())
			{
			SystemTray tray = SystemTray.getSystemTray();
			PopupMenu popup= new PopupMenu("SciFOAF");
			popup.add(new MenuItem("Menu1"));
			BufferedImage image= new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);
			 trayIcon = new TrayIcon(image, "SciFOAF", popup);
			 try {
	             tray.add(trayIcon);
	         } catch (AWTException e) {
	             Debug.debug(e);
	         }
			}
		else
			{
			Debug.debug();
			}
		
		this.addWindowListener(new WindowAdapter()
			{
			@Override
			public void windowClosing(WindowEvent e) {
				doMenuQuit();
				}
			});
		JMenuBar bar= new JMenuBar();
		setJMenuBar(bar);
		JMenu menu= new JMenu("File");
		bar.add(menu);
		
		
		menu.add(new JMenuItem(new AbstractAction("About")
				{
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(SciFOAFApplication.this,
							new JLabel("<html><body>"+
							"<h1 align=\"center\">SciFOAF 2.0</h1>"+
							"<h2 align=\"center\">Pierre Lindenbaum PhD 2007</h2>"+
							"<h3 align=\"center\">"+Compilation.getLabel()+"</h3>"+
							"</body></html>"),"About..",JOptionPane.PLAIN_MESSAGE,null);
					}
				}));
		
		menu.add(new JSeparator());	
				
		menu.add(new JMenuItem(this.menuNewAction=new AbstractAction("New")
			{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				doMenuNew();
				}
			}));
		menu.add(new JMenuItem(this.menuOpenAction=new AbstractAction("Open")
			{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				doMenuOpen();
				}
			}));
		menu.add(new JMenuItem(this.menuCloseAction=new AbstractAction("Close")
			{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				doMenuClose();
				}
			}));
		menu.add(new JSeparator());
		menu.add(new JMenuItem(new AbstractAction("Quit")
			{
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				doMenuQuit();	
				}
			}));
		
		this.windowsMenu= new JMenu("Windows");
		bar.add(this.windowsMenu);
		
		
		JPanel contentPane= new JPanel(new BorderLayout());
		setContentPane(contentPane);
		this.desktopPane= new  JDesktopPane();
		contentPane.add(this.desktopPane,BorderLayout.CENTER);
		
		
		this.derbyModel.addObserver(new Observer()
			{
			@Override
			public void update(Observable o, Object arg) {
				menuNewAction.setEnabled(arg==null);
				menuOpenAction.setEnabled(arg==null);
				menuCloseAction.setEnabled(arg!=null);
			
				if(arg!=null)
					{
					StatementsFrame f= new StatementsFrame();
					f.setClosable(false);
					f.setTitle("All Statements");
					f.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
					desktopPane.add(f);
					f.setVisible(true);
					}
				fireRDFModelUpdated();
				}
			});
		this.derbyModel.fireValueChanged();
		SwingUtils.center(this, 50);
		}


	
	private void updateMenuWindow()
		{
		this.windowsMenu.removeAll();
		for(JInternalFrame f: this.desktopPane.getAllFrames())
			{
			this.windowsMenu.add(new JMenuItem(new ObjectAction<JInternalFrame>(f,f.getTitle())
				{
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e)
					{
					if(getObject().isIcon())
						{
						try {getObject().setIcon(false);} catch (PropertyVetoException e1) {}
						}
					getObject().toFront();
					}
				}));
			}
		}
	
	private void doMenuNew()
		{
		JFileChooser chooser=new JFileChooser(PreferredDirectory.getPreferredDirectory());
		if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
		File f= chooser.getSelectedFile();
		if(f==null) return;
		if(f.exists())
			{
			JOptionPane.showMessageDialog(this, f.toString()+" exist.", "Exsts", JOptionPane.WARNING_MESSAGE, null);
			return;
			}
		
		PreferredDirectory.setPreferredDirectory(f);
		
		try {
			FoafModel model= new FoafModel(f);
			
			try {
				if(this.derbyModel.hasValue())
					{
					this.derbyModel.getValue().close();
					}
				} catch (SQLException e) {ThrowablePane.show(this, e); }
			this.derbyModel.setValue(model);
			} 
		catch (Exception e2) {
			ThrowablePane.show(this, e2);
			}
		}
	
	private void doMenuOpen()
		{
		JFileChooser chooser=new JFileChooser(PreferredDirectory.getPreferredDirectory());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
		File f= chooser.getSelectedFile();
		if(f==null) return;
		PreferredDirectory.setPreferredDirectory(f);
		try {
			FoafModel model= new FoafModel(f);
			try {
				if(this.derbyModel.hasValue())
					{
					this.derbyModel.getValue().close();
					}
				} catch (SQLException e) {ThrowablePane.show(this, e); }
			this.derbyModel.setValue(model);
			} 
		catch (Exception e2) {
			ThrowablePane.show(this, e2);
			}
		}
	
	private void doMenuQuit()
		{
		doMenuClose();
		this.setVisible(false);
		this.dispose();
		}
	
	private void doMenuClose()
		{
		try {
			if(this.derbyModel.hasValue())
				{
				this.derbyModel.getValue().close();
				}
			}
		catch (Exception e) {
			ThrowablePane.show(this, e);
			}
		for(JInternalFrame f:this.desktopPane.getAllFrames())
			{
			f.setVisible(false);
			f.dispose();
			}
		this.desktopPane.removeAll();
		this.derbyModel.setValue(null);
		}
	
	private void fireRDFModelUpdated()
		{
		for(JInternalFrame f: this.desktopPane.getAllFrames())
			{
			if(!StatementsFrame.class.isInstance(f)) continue;
			StatementsFrame.class.cast(f).fireRDFModelUpdated();
			}
		}
	
	private FoafModel getRDFModel()
		{
		return this.derbyModel.getValue();
		}
	
	private void doMenuNewPaper(Component owner)
		{
		SimpleDialog dialog= new SimpleDialog(this,"Enter a PMID");
		JPanel pane= new JPanel(new InputLayout());
		dialog.getContentPane().add(pane);
		pane.add(new JLabel("Enter a Pubmed Identifier (PMID)",JLabel.RIGHT));
		JTextField f= new JTextField(10);
		if(Debug.isDebugging()) f.setText("9682060");
		f.setName("PMID");
		dialog.getOKAction().mustBeInRange(f,1,Integer.MAX_VALUE);
		pane.add(f);
		if(dialog.showDialog()!=SimpleDialog.OK_OPTION) return;
		int pmid= Integer.parseInt(f.getText().trim());
		
		try
			{
			if(getRDFModel().containsSubject(getRDFModel().createResource(Paper.getURI(""+pmid))))
				{	
				JOptionPane.showMessageDialog(owner, "Alredy in Model : PMID ID."+pmid);
				return;
				}
			}
		catch(SQLException err)
			{
			ThrowablePane.show(owner, err);
			}
		loadPubmedPapersByPMID(this,pmid);
		}
	
	private void loadPubmedPapersByPMID(Component owner, int pmid)
		{
		try {
			class Param extends WindowAdapter
				{
				int pmid;
				JDialog dialog;
				Document dom;
				Throwable error=null;
				
				@Override
				public void windowOpened(WindowEvent e)
					{
					Runnable runner= new Runnable()
						{
						@Override
						public void run()
							{
							try {	
								DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
								factory.setCoalescing(true);
								factory.setExpandEntityReferences(true);
								factory.setValidating(false);
								factory.setIgnoringComments(true);
								factory.setIgnoringElementContentWhitespace(true);
								factory.setXIncludeAware(false);
								DocumentBuilder builder= factory.newDocumentBuilder();
							
							
								String url="http://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id="+
								Param.this.pmid+"&retmode=xml";
								Debug.debug(url);
								Param.this.dom=builder.parse(url);
								Debug.debug("OK, downloaded");
								
								}
							catch (Exception e)
								{
								Debug.debug(e);
								Param.this.error=e;
								Param.this.dom=null;
								}
							Debug.debug("End Thread");
							Param.this.dialog.setVisible(false);
							Param.this.dialog.dispose();
							}	
						};
					Thread t= new Thread(runner);
					t.start();
					}
				
				}
			Param param= new Param();
			param.pmid=pmid;
			
			
			
			param.dialog= new JDialog(owner==null?null:SwingUtilities.getWindowAncestor(owner),"Fetching Paper",Dialog.ModalityType.APPLICATION_MODAL);
			param.dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			JPanel pane= new JPanel(new BorderLayout());
			pane.setBorder(new EmptyBorder(10,10,10,10));
			param.dialog.setContentPane(pane);
			pane.add(SwingUtils.withFont(new JLabel(" Fetching PMID."+pmid+" from NCBI ",JLabel.CENTER),new Font("Dialog",Font.BOLD,24)),BorderLayout.NORTH);
			JProgressBar bar= new JProgressBar();
			bar.setIndeterminate(true);
			pane.add(bar,BorderLayout.CENTER);
			SwingUtils.packAndCenter(param.dialog);
			param.dialog.addWindowListener(param);
			param.dialog.setVisible(true);
			
			if(param.dom!=null)
				{
				Element root=param.dom.getDocumentElement();
				if(root!=null && !root.getNodeName().equals("PubmedArticleSet"))
					{
					param.error= new IOException(root.getNodeName()+":"+root.getTextContent());
					param.dom=null;
					}
				}
			
			if(param.error!=null)
				{
				ThrowablePane.show(owner, param.error);
				return;
				}
			
			Debug.debug("Done");
			Paper test=Paper.parse(param.dom.getDocumentElement());
			PaperEditor ed= new PaperEditor(owner,test)
				{
				private static final long serialVersionUID = 1L;

				@Override
				FoafModel getModel() {
					return SciFOAFApplication.this.getRDFModel();
					}
				};
			if(ed.showDialog()!=PaperEditor.OK_OPTION) return;
			ed.create();
			fireRDFModelUpdated();
			}
		catch (Exception e) {
			ThrowablePane.show(owner, e);
			return;
			}
		}
	
	
	private BufferedImage loadDepiction(DerbyModel.Resource foafAgent) throws IOException,SQLException
		{
		BufferedImage img=null;
		CloseableIterator<DerbyModel.Statement> iter1= getRDFModel().listStatements(
			foafAgent,
			getRDFModel().createResource(FOAF.NS,"depicted")
			, null);
		
		while(iter1.hasNext())
			{
			DerbyModel.Statement stmt1= iter1.next();
			if(!stmt1.getValue().isResource()) continue;
			DerbyModel.Resource imgrsrc= stmt1.getValue().asResource();
			if(!imgrsrc.hasProperty(
				getRDFModel().RDF_TYPE,
				getRDFModel().createResource(FOAF.NS, "Image")	
				)) continue;
			String filetype= imgrsrc.getURI().toLowerCase();
			
			
			if(!imgrsrc.isURL()) continue;
			URL url= imgrsrc.asURL();
			if(url==null) continue;
			
			
			}
		
		iter1.close();
		return img;
		}
	
	
	/**
	 * main
	 */
	public static void main(String[] args) {
		try {
			Debug.setDebugging(true);
			JFrame.setDefaultLookAndFeelDecorated(true);
			JDialog.setDefaultLookAndFeelDecorated(true);
	    	int optind=0;
	    	
	    	while(optind<args.length)
			        {
			        if(args[optind].equals("-h"))
			           {
			        	System.err.println(Compilation.getLabel());
			        	System.err.println("\t-h this screen");
			        	System.err.println("\t-d turns debugging on");
						return;
			           	}
			        else if(args[optind].equals("-d"))
			        	{
			        	Debug.setDebugging(true);
			        	}
			       else if(args[optind].equals("--"))
			            {
			            ++optind;
			            break;
			            }
			        else if(args[optind].startsWith("-"))
			            {
			            throw new IllegalArgumentException("Unknown option "+args[optind]);
			            }
			        else
			            {
			            break;    
			            }
			        ++optind;
			        }
	    	File fileIn=null;
	    	
	    	if(optind+1==args.length)
	    		{	
	    		fileIn= new File(args[++optind]);
				}
	    	else if(optind!=args.length)
	    		{	
	    		throw new IllegalArgumentException("Bad number of argments.");
				}
	    	
	    	try {
	    		Class.forName(DerbyModel.JDBC_DRIVER_NAME);
	    		} 
	    	catch (ClassNotFoundException e)
	    		{
	    		JOptionPane.showMessageDialog(null,
	    				"<html><body><h1>Cannot find the SQL Driver "+DerbyModel.JDBC_DRIVER_NAME+
	    				" in the java $CLASSPATH</h1>"+
	    				"<p>See <a href='http://developers.sun.com/javadb/'>http://developers.sun.com/javadb/</a></p>"+
	    				"</body></html>",
	    				e.getClass().getName(),
	    				JOptionPane.ERROR_MESSAGE,
	    				null
	    				);
	    		return;
	    		}
	    	
	    		
			SwingUtilities.invokeAndWait(new RunnableObject<File>(fileIn)
				{
				@Override
				public void run()
					{
					SciFOAFApplication win= new SciFOAFApplication();
					win.setVisible(true);
					}
				});
				
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}

}
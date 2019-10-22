package invoice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.sql.DataSource;

public class DAO {

	private final DataSource myDataSource;

	/**
	 *
	 * @param dataSource la source de données à utiliser
	 */
	public DAO(DataSource dataSource) {
		this.myDataSource = dataSource;
	}

	/**
	 * Renvoie le chiffre d'affaire d'un client (somme du montant de ses factures)
	 *
	 * @param id la clé du client à chercher
	 * @return le chiffre d'affaire de ce client ou 0 si pas trouvé
	 * @throws SQLException
	 */
	public float totalForCustomer(int id) throws SQLException {
		String sql = "SELECT SUM(Total) AS Amount FROM Invoice WHERE CustomerID = ?";
		float result = 0;
		try (Connection connection = myDataSource.getConnection();
			PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, id); // On fixe le 1° paramètre de la requête
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					result = resultSet.getFloat("Amount");
				}
			}
		}
		return result;
	}

	/**
	 * Renvoie le nom d'un client à partir de son ID
	 *
	 * @param id la clé du client à chercher
	 * @return le nom du client (LastName) ou null si pas trouvé
	 * @throws SQLException
	 */
	public String nameOfCustomer(int id) throws SQLException {
		String sql = "SELECT LastName FROM Customer WHERE ID = ?";
		String result = null;
		try (Connection connection = myDataSource.getConnection();
			PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					result = resultSet.getString("LastName");
				}
			}
		}
		return result;
	}

	/**
	 * Transaction permettant de créer une facture pour un client
	 *
	 * @param customer Le client
	 * @param productIDs tableau des numéros de produits à créer dans la facture
	 * @param quantities tableau des quantités de produits à facturer faux sinon Les deux tableaux doivent avoir la même
	 * taille
	 * @throws java.lang.Exception si la transaction a échoué
	 */
	public void createInvoice(CustomerEntity customer, int[] productIDs, int[] quantities) throws Exception {  
            if (productIDs.length != quantities.length)
                throw new IllegalArgumentException("arrays must have the same length");
            
            String sql = "INSERT INTO Invoice (CustomerID, Total) VALUES (?, ?)";
            String productsSql = "SELECT * FROM Product WHERE ID = ?";
            List<Product> products = new ArrayList<Product>();
            
            try (Connection connection = myDataSource.getConnection();
                    PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < productIDs.length; i++) {
                    try (Connection productConnection = myDataSource.getConnection();
                        PreparedStatement productStmt = productConnection.prepareStatement(productsSql)){
                        
                        productStmt.setInt(1, i);
                        ResultSet rs = productStmt.executeQuery();
                        
                        if (rs.next() && quantities[i] >= 0) {
                            products.add(new Product(i, 
                                                    rs.getString("Name"), 
                                                    rs.getFloat("Price"),
                                                    quantities[i]));
                        } else throw new IllegalArgumentException("Product not found or negative quantity");
                    }   
                } 
                
                float total = 0.f;
                
                for (Product p : products)
                    total += p.getTotalPrice();
                
                stmt.setInt(1, customer.getCustomerId());
                stmt.setFloat(2, total);
               
                stmt.executeUpdate();
            }
            
            sql = "SELECT MAX(ID) as id FROM Invoice";
            
            int invoiceId = -1;
            
            try (Connection connection = myDataSource.getConnection();
                    PreparedStatement stmt = connection.prepareStatement(sql))
            {
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    invoiceId = rs.getInt("id");   
            }
            
            sql = "INSERT INTO Item (Item, InvoiceID, ProductID, Quantity, Cost) VALUES(?, ?, ?, ?, ?)";

               for (int i = 0; i < products.size()-1; i++)
                   sql += ", (?, ?, ?, ?, ?)";
                
            try ( Connection connetion = myDataSource.getConnection();
                    PreparedStatement stmt = connetion.prepareStatement(sql)
                    ) 
            {
                for (int i = 0; i < products.size(); i++) {
                    Product p = products.get(i);
                    
                    stmt.setInt(i+1, i);
                    stmt.setInt(i+2, invoiceId);
                    stmt.setInt(i+3, p.getID());
                    stmt.setInt(i+4, p.getQuantity());
                    stmt.setFloat(i+5, p.getTotalPrice()); 
                }
                
                stmt.executeUpdate();
            }
	}

	/**
	 *
	 * @return le nombre d'enregistrements dans la table CUSTOMER
	 * @throws SQLException
	 */
	public int numberOfCustomers() throws SQLException {
		int result = 0;

		String sql = "SELECT COUNT(*) AS NUMBER FROM Customer";
		try (Connection connection = myDataSource.getConnection();
			Statement stmt = connection.createStatement()) {
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				result = rs.getInt("NUMBER");
			}
		}
		return result;
	}

	/**
	 *
	 * @param customerId la clé du client à recherche
	 * @return le nombre de bons de commande pour ce client (table PURCHASE_ORDER)
	 * @throws SQLException
	 */
	public int numberOfInvoicesForCustomer(int customerId) throws SQLException {
		int result = 0;

		String sql = "SELECT COUNT(*) AS NUMBER FROM Invoice WHERE CustomerID = ?";

		try (Connection connection = myDataSource.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, customerId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = rs.getInt("NUMBER");
			}
		}
		return result;
	}

	/**
	 * Trouver un Customer à partir de sa clé
	 *
	 * @param customedID la clé du CUSTOMER à rechercher
	 * @return l'enregistrement correspondant dans la table CUSTOMER, ou null si pas trouvé
	 * @throws SQLException
	 */
	CustomerEntity findCustomer(int customerID) throws SQLException {
		CustomerEntity result = null;

		String sql = "SELECT * FROM Customer WHERE ID = ?";
		try (Connection connection = myDataSource.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setInt(1, customerID);

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String name = rs.getString("FirstName");
				String address = rs.getString("Street");
				result = new CustomerEntity(customerID, name, address);
			}
		}
		return result;
	}

	/**
	 * Liste des clients localisés dans un état des USA
	 *
	 * @param state l'état à rechercher (2 caractères)
	 * @return la liste des clients habitant dans cet état
	 * @throws SQLException
	 */
	List<CustomerEntity> customersInCity(String city) throws SQLException {
		List<CustomerEntity> result = new LinkedList<>();

		String sql = "SELECT * FROM Customer WHERE City = ?";
		try (Connection connection = myDataSource.getConnection();
			PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, city);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					int id = rs.getInt("ID");
					String name = rs.getString("FirstName");
					String address = rs.getString("Street");
					CustomerEntity c = new CustomerEntity(id, name, address);
					result.add(c);
				}
			}
		}

		return result;
	}
}

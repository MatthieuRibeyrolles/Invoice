/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package invoice;

/**
 *
 * @author Matthieu
 */
public class Product {
    private int ID;
    private String name;
    private float totalPrice;
    private float price;
    private int quantity;

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public float getTotalPrice() {
        return totalPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getPrice() {
        return price;
    }
    
    

    public Product(int ID, String name, float price, int quantity) {
        this.ID = ID;
        this.name = name;
        this.totalPrice = quantity * price;
        this.price = price;
        this.quantity = quantity;
    }
}

// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------
package ResImpl;

public class Hotel extends ReservableItem
{
    public Hotel( String location, int count, int price )
    {
        super( location, count, price );
    }

    public Hotel( Hotel copyHotel )
    {
        super( copyHotel.getLocation(), copyHotel.getCount(), copyHotel.getPrice() );
        this.setReserved(copyHotel.getReserved());
    }

    public String getKey()
    {
        return Hotel.getKey( getLocation() );
    }

    public static String getKey( String location )
    {
        String s = "room-" + location  ;
        return s.toLowerCase();
    }
}


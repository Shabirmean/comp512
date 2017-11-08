package MiddlewareImpl;

public class Hotel extends ReservableItem
{
    public Hotel( String location, int count, int price )
    {
        super( location, count, price );
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

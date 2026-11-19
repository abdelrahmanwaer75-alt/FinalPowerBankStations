// Sample station data. Names, addresses, and coordinates match database/sample_data.sql.
// `available` and `returnSlots` are demo values until the backend API is connected.
// Price comes from the active pricing plan (8 EGP per hour in the sample data).
window.STATIONS_DATA = [
    { id: 1, name: 'Central Station', address: 'Talaat Harb St, Cairo', lat: 30.0444, lng: 31.2357, hours: '24 hours', total: 4, available: 4, returnSlots: 0, price: 8 },
    { id: 2, name: 'Zamalek Cafe Corner', address: '26th of July St, Cairo', lat: 30.0610, lng: 31.2190, hours: '08:00 - 23:00', total: 4, available: 2, returnSlots: 2, price: 8 },
    { id: 3, name: 'Nasr City Mall', address: 'Abbas El Akkad St, Cairo', lat: 30.0626, lng: 31.3300, hours: '10:00 - 00:00', total: 4, available: 3, returnSlots: 1, price: 8 },
    { id: 4, name: 'Maadi Road 9', address: 'Road 9, Cairo', lat: 29.9602, lng: 31.2569, hours: '09:00 - 22:00', total: 4, available: 0, returnSlots: 4, price: 8 },
    { id: 5, name: 'Heliopolis Korba', address: 'Korba St, Cairo', lat: 30.0900, lng: 31.3250, hours: '09:00 - 23:00', total: 4, available: 1, returnSlots: 3, price: 8 },
    { id: 6, name: 'Giza Pyramids Road', address: 'Pyramids Road, Giza', lat: 29.9870, lng: 31.2118, hours: '08:00 - 20:00', total: 4, available: 4, returnSlots: 0, price: 8 },
    { id: 7, name: 'Sheikh Zayed Mall', address: 'Sheikh Zayed City Mall, Giza', lat: 30.0130, lng: 30.9900, hours: '10:00 - 00:00', total: 4, available: 2, returnSlots: 2, price: 8 },
    { id: 8, name: 'Alamein Campus', address: 'Alamein International University', lat: 30.8397, lng: 28.9550, hours: '07:00 - 21:00', total: 4, available: 4, returnSlots: 0, price: 8 },
    { id: 9, name: 'Marina Boardwalk', address: 'Marina 1, North Coast', lat: 30.8300, lng: 28.9400, hours: '10:00 - 02:00', total: 4, available: 3, returnSlots: 1, price: 8 }
];

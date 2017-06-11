# ShadowHashMap
A very fast HashMap with close to O(1) time complexity in both directions without taking up twice as much memory.

A normal hash map has a O(1) time complexity for finding the value from a certain key by calculating the index for the entry containing
that key (index = key % size). This is not true the other way around though. In order to find a key or keys from a certain value you
normally need to go through your entire hash table i.e. O(n) time complexity. In order to make that O(1) I added a reference in the entry
at index = value % size pointing to the entry containing that value. If there is no entry at that index, an empty one will be created.
This entry containing the reference to the other entry containing that value is added when a key-value pair is added to the map. I say that
a the entry casts a shadow onto another position in the table. This is a simplification of the process since you have to take into 
account that you can have many keys associated with the same value. I have added a method for retrieving ALL the keys from a certain value.

So why is this good? Imagine that you want to build a database of students and their grades. Normally you would use a hash map that takes
the student ID as the key and the grade as their value. You would then be able to retrieve the value from a students ID immediately.
However, imagine that you now want to see which students got a 5. You would now have to search through all the students in order to find
them. With a shadow hash map you can just call getKeys(5) and it will immediately retrieve the student that got a 5. Not only that, if you
take getKeys(5).size() on that you get a histogram, i.e. the number of students who got a 5.

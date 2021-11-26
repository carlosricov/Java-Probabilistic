// Carlos Ricoveri

//SkipList.java -- A probabilistic data structure .

import java.util.*;
import java.io.*;

class Node<T extends Comparable<T>>
{
  T data;
  int height;

  // The levels for each node will be stored in an arraylist.
  ArrayList<Node<T>> ref = new ArrayList<>();

  // Constructor for initial list setup.
  Node(int height)
  {
    // The head of a SkipList does not store data.
    this.data = null;

    this.height = height;

    // Crucial to initialize the references to null for a new Skiplist.
    for (int i = 0; i < height; i++)
    {
      ref.add(null);
    }
  }

  // Constructor used for node insertion.
  Node(T data, int height)
  {
    this.data = data;
    this.height = height;

    // Update next node pointers.
    for (int i = 0; i < height; i++)
    {
      ref.add(null);
    }
  }

  public T value()
  {
    return this.data;
  }

  public int height()
  {
    return this.height;
  }

  // Function that returns the reference at a particular level.
  public Node<T> next(int level)
  {
    // Out of bound checks.
    if (level < 0 || level >= height)
      return null;

    return ref.get(level);
  }

  // Function that updates the reference at a level in this node.
  public void setNext(int level, Node<T> node)
  {
    // Out of bound check.
    if (level < 0 || level >= height)
      return;

    ref.set(level, node);
  }

  // Function that grows the heights when needed.
  public void grow()
  {
    this.height++;
    ref.add(null);
  }

  // If the max height of the Skiplist is increased, there's a 50% chance of
  // height increase for all nodes.
  public void maybeGrow()
  {
    // Range: [0,1)
    double rand = Math.random();

    if (rand >= 0.5)
    {
      this.height++;
      ref.add(null);
    }
  }

  // Function that trims the node to the specified height.
  public void trim(int height)
  {
    // Out of bound check.
    if (height < 0 || height >= this.height)
      return;

    // Set the top most references to null.
    for (int i = this.height; i > height; i--)
    {
      ref.remove(this.height - 1);
      this.height--;
    }
  }
}

// Skiplist class.
public class SkipList<T extends Comparable<T>>
{
  int maxHeight, size = 0;

  // This boolean variable indicates whether or not the skiplist was created
  // with a designated height.
  boolean manuallyCreated;

  Node<T> head;

  // Constructor that initializes an empty Skiplist with a default height of 1.
  SkipList()
  {
    this.maxHeight = 1;
    this.manuallyCreated = false;
    this.head = new Node<T>(maxHeight);
  }

  // Constructor that initializes the Skiplist with a set max height.
  SkipList(int height)
  {
    // Valid height check.
    if (height < 1)
      this.maxHeight = 1;
    else
      this.maxHeight = height;
    
    this.manuallyCreated = true;
    this.head = new Node<T>(maxHeight);
  }

  // Function that returns the size of the Skiplist.
  public int size()
  {
    return this.size;
  }

  // Function that returns the max height of the Skiplist.
  public int height()
  {
    return this.maxHeight;
  }

  // Function that returns the head of the Skiplist.
  public Node<T> head()
  {
    return head;
  }

  // Helper function that returns the max height of a Skiplist with n nodes.
  private static int getMaxHeight(int n)
  {
    // Max height = log base 2 of n and take the ceiling of it.
    double height = (Math.log(n)/Math.log(2));

    return (n == 1) ? 1 : (int)Math.ceil(height);
  }

  // Helper function that generates a probabilistic height for a node.
  private static int generateRandomHeight(int maxHeight)
  {
    double rand = Math.random();
    int height = 1;

    // 50% for each iteration, abiding to the max height.
    while (rand > 0.5 && height < maxHeight)
    {
      height++;
      rand = Math.random();
    }

    return height;
  }

  // Grow the skiplist when the max possible height is updated.
  private void growSkipList()
  {
    // Keep track of the old max height.
    int oldHeadHeight = head.height();
    
    Node<T> prevSpot = head;
    Node<T> spot;

    // Indicators if the height of the node grew.
    int spotHeight, newHeight;

    // The height of the head will always increment when growing the skiplist.
    head.grow();
    
    // Update references.
    spot = prevSpot.next(oldHeadHeight - 1);

    while (spot != null)
    {
      spotHeight = spot.height();

      // Every node (besides the head) with a height of the old max height has
      // a 50% chance of growing.
      if (spotHeight == oldHeadHeight)
        spot.maybeGrow();

      newHeight = spot.height();

      // Check if node increased in height.
      if (newHeight - spotHeight == 1)
      {
        // Update references.
        prevSpot.setNext(prevSpot.height() - 1, spot);
        
        prevSpot = spot;
        spot = spot.next(spot.height() - 2);
      }
      else
      {
        spot = spot.next(spot.height() - 1);
      }
    }

    // Increase max height of the skiplist 
    this.maxHeight++;
  }

  // If necessary, the max height of the skip list will decrease with deletion.
  // Any node that's left taller than the new height must be trimmed.
  private void trimSkipList()
  {
    int expectedHeight = getMaxHeight(this.size);

    if (manuallyCreated && this.maxHeight > expectedHeight)
      this.maxHeight = expectedHeight;
    else
      this.maxHeight--;

    int newHeight = this.maxHeight;

    Node<T> prevSpot = head;
    Node<T> spot = head.next(newHeight);
    head.trim(newHeight);

    while (spot != null)
    {
        prevSpot = spot;
        spot = spot.next(newHeight);
        prevSpot.trim(newHeight);
    }
  }

  // Function that inserts a node into the Skiplist in O(logn) time.
  public void insert(T data)
  {
    int oldMaxHeight = this.maxHeight;

    // Probabilisitic generation of a node's height.
    int nodeHeight = generateRandomHeight(this.maxHeight);

    // A red dot is a point where we drop down one level in a node.
    ArrayList<Node<T>> redDot = new ArrayList<Node<T>>();

    Node<T> node = new Node<T>(data, nodeHeight);
    Node<T> tempNode = head;

    // Traversal begins at the head of the skiplist which will always have the
    // max height of the list.
    for (int i = head.height() - 1; i >= 0; i--)
    {
      // In the case where the list is empty or we've reached the last node,
      // its last reference should point to null.
      while (tempNode != null)
      {
        // A null reference within the list causes us to drop down a level.
        if (tempNode.next(i) == null)
        {
          if (i < node.height())
          {
            redDot.add(tempNode);
          }
          
          break;
        }
        // In the case that the level points to a value bigger than or equal to
        // what we're inserting, we drop down a level.
        else if (tempNode.next(i).value().compareTo(data) >= 0)
        {
          // Update references.
          if (i < node.height())
          {
            redDot.add(tempNode);
          }

          break;
        }
        // In the case that the level points to a value smaller than what we're
        // inserting, then we proceed to the node containing that value.
        else if (tempNode.next(i).value().compareTo(data) < 0)
        {
          tempNode = tempNode.next(i);
          continue;
        }
      }
    }

    int cnt = redDot.size() - 1 ;

    // This for loop updates the next references of the new node.
    for (int j = 0; j < redDot.size(); j++)
    {
      node.setNext(j, redDot.get(cnt).next(j));
      cnt--;
    }

    cnt = redDot.size() - 1 ;

    // Update references for new insertion.
    for (int k = 0; k < redDot.size(); k++)
    {
        redDot.get(k).setNext(cnt, node);
        cnt--;
    }
    
    if (size == 0)
      head.setNext(0, node);

    // Insertion causes the size of the skiplist to increment by one.
    this.size++;

    // After insertion it's important to check if the max possible height for
    // the list has changed at all.
    int newMaxHeight = getMaxHeight(this.size);

    // If the max possible height changes, the list must grow.
    if ((newMaxHeight - oldMaxHeight) > 0)
      growSkipList();
    else
      return;
  }

  // Insert function that allows the custom input of node heights.
  public void insert(T data, int height)
  {
    int oldMaxHeight = this.maxHeight;
    int nodeHeight = height;

    ArrayList<Node<T>> redDot = new ArrayList<Node<T>>();
    Node<T> node = new Node<T>(data, nodeHeight);
    Node<T> tempNode = head;

    // Traversal begins at the head and stops once we've dropped from the
    // lowest level.
    for (int i = head.height() - 1; i >= 0; i--)
    {
      while (tempNode != null)
      {
        if (tempNode.next(i) == null)
        {
          if (i < node.height())
          {
            redDot.add(tempNode);
          }

          break;
        }
        else if (tempNode.next(i).value().compareTo(data) >= 0)
        {
          if (i < node.height())
          {
            redDot.add(tempNode);
          }

          break;
        }
        else if (tempNode.next(i).value().compareTo(data) < 0)
        {
          tempNode = tempNode.next(i);
          continue;
        }
      }
    }

    int cnt = redDot.size() - 1 ;

    for (int j = 0; j < redDot.size(); j++)
    {
      node.setNext(j, redDot.get(cnt).next(j));
      cnt--;
    }

    cnt = redDot.size() - 1 ;

    for (int k = 0; k < redDot.size(); k++)
    {
      redDot.get(k).setNext(cnt, node);
      cnt--;
    }

    if (size == 0)
      head.setNext(0, node);

    this.size++;
    int newMaxHeight = getMaxHeight(this.size);

    if ((newMaxHeight - oldMaxHeight) > 0)
      growSkipList();
    else
      return;
  }

  // Method that deletes a node from the list in O(logn) time.
  public void delete(T data)
  {
    // Initial check to see if the data is in the list.
    if (!contains(data))
      return;

    // Node to be deleted.
    Node<T> spot = get(data);
    
    int oldMaxHeight = this.maxHeight;
    
    ArrayList<Node<T>> redDot = new ArrayList<Node<T>>();
    Node<T> tempNode = head;

    // Same procedure as insertion. Start at the head; drop when needed.
    for (int i = head.height() - 1; i >= 0; i--)
    {
      while (tempNode != null)
      {
        if (tempNode.next(i) == null)
        {
          break;
        }
        else if (tempNode.next(i).value().compareTo(data) == 0)
        {
          if (i < spot.height())
          {
            redDot.add(tempNode);
          }

          break;
        }
        else if (tempNode.next(i).value().compareTo(data) > 0)
        {
          break;
        }
        else if (tempNode.next(i).value().compareTo(data) < 0)
        {
          tempNode = tempNode.next(i);
          continue;
        }
      }
    }

    int cnt = redDot.size() - 1 ;

    for (int j = 0; j < redDot.size(); j++)
    {
      redDot.get(cnt).setNext(j, spot.next(j));
      cnt--;
    }

    this.size--;
    int newMaxHeight = getMaxHeight(this.size);

    // If the max possible height changes, the list must shrink.
    if ((newMaxHeight - oldMaxHeight) < 0)
      trimSkipList();
    else
      return;
  }

  // Function that indicates if a node is present in the list.
  public boolean contains(T data)
  {
    Node<T> tempNode = head;

    for (int i = head.height() - 1; i >= 0; i--)
    {
      while (tempNode != null)
      {
        if (tempNode.next(i) == null)
        {
          break;
        }
        else if (tempNode.next(i).value().compareTo(data) > 0)
          break;
        else if (tempNode.next(i).value().compareTo(data) < 0)
        {
          tempNode = tempNode.next(i);
          continue;
        }
        // Bingo--we have found the node.
        else if (tempNode.next(i).value().compareTo(data) == 0)
        {
          return true;
        }
      }
    }
    
    // No value found.
    return false;
  }

  // Function that returns the node containing the passed data.
  public Node<T> get(T data)
  {
    Node<T> tempNode = head;

    for (int i = head.height() - 1; i >= 0; i--)
    {
      while (tempNode != null)
      {
        if (tempNode.next(i) == null)
        {
          break;
        }
        else if (tempNode.next(i).value().compareTo(data) > 0)
          break;
        else if (tempNode.next(i).value().compareTo(data) < 0)
        {
          tempNode = tempNode.next(i);
          continue;
        }
        // We have found the node!
        else if (tempNode.next(i).value().compareTo(data) == 0)
        {
          return tempNode.next(i);
        }
      }
    }

    // No node found.
    return null;
  }
}

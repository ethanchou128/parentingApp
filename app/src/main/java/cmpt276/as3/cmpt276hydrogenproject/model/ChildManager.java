package cmpt276.as3.cmpt276hydrogenproject.model;

import java.util.ArrayList;

public class ChildManager {
    private ArrayList<Child> CHILDREN_LIST = new ArrayList<>();
    private static ChildManager instance;

    /**
     * Method to retrieve the class without accessing it by the constructor
     * @return the only instance of the class
     */
    public static ChildManager getInstance() {
        if (instance == null) {
            instance = new ChildManager();
        }
        return instance;
    }

    private ChildManager() {}

    public Child getChildAt(int index) {
        return CHILDREN_LIST.get(index);
    }

    public Child getFirstChild() {
        return CHILDREN_LIST.get(0);
    }

    //suggests a child to pick
    public Child getNextChild(Child previousPick) {
        int index = CHILDREN_LIST.indexOf(previousPick);
        index++;
        if (index == CHILDREN_LIST.size()) {
            index = 0;
        }
        return CHILDREN_LIST.get(index);
    }

    public int indexOfChild(Child child) {
        return CHILDREN_LIST.indexOf(child);
    }

    public ArrayList<Child> getChildrenList() {
        return CHILDREN_LIST;
    }

    public void setAllChildren(ArrayList<Child> childList) {
        this.CHILDREN_LIST = childList;
    }

    public int getSizeOfChildList() { return CHILDREN_LIST.size(); }

    public void addChild(String name) {
        Child child = new Child(name);
        CHILDREN_LIST.add(child);
    }

    public boolean containsChild (Child child) {
        return CHILDREN_LIST.contains(child);
    }

    public boolean isEmpty() {
        return CHILDREN_LIST.isEmpty();
    }

    public void removeChildByIdx(int idx) {
        CHILDREN_LIST.remove(idx);
    }

    public void removeChildByObject(Child child) {
        CHILDREN_LIST.remove(child);
    }

    public void editChildName(int idx, String name) {
        Child child = CHILDREN_LIST.get(idx);
        child.setName(name);
    }

    /**
     * Determines if an inputted name is valid.
     * Names with no characters and names with all spaces are invalid.
     */
    public static boolean isValidName(String name) {
        if (name.length() == 0) {
            return false;
        }

        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) != ' ') {
                return true;
            }
        }
        return false;
    }
}
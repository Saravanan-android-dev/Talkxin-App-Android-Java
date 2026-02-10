using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class gameman : MonoBehaviour
{
    // Start is called before the first frame update
    public int count=0;
    public GameObject x;
    public GameObject y;
    void Start()
    {
        
    }

    // Update is called once per frame
    void Update()
    {
        if (count % 2 == 0)
        {
            x.SetActive(true);
            y.SetActive(false);
        }
        else
        {
            x.SetActive(false);
            y.SetActive(true);
        }
    }
}

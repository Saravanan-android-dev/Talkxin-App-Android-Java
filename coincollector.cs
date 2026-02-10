using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class coincollector : MonoBehaviour
{
    // Start is called before the first frame update
    public int point = 0;
    public Text points;
    void Start()
    {
        
    }
    private void OnTriggerEnter2D(Collider2D collision)
    {
        if (collision.gameObject.tag == "coins")
        {
            Destroy(collision.gameObject);
       

           
        }
       

    }
    // Update is called once per frame
    void Update()
    {
       
    }
}

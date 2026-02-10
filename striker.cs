using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class striker : MonoBehaviour
{
    // Start is called before the first frame update
    Rigidbody2D rigidbody;
    Transform selftrans;
    Vector2 startpos;
    public Slider myslider;
    Vector2 direction;
    Vector3 mousepos;
    Vector3 mouspos2;
    public LineRenderer line;
    bool hasStriked=false;
    bool positionset = false;
    public GameObject bord;
    void Start()
    {
        rigidbody = GetComponent<Rigidbody2D>();
        selftrans = transform;
        startpos = transform.position;
    }
    public void shootstriker()
    {
        float x = 0;
        if(positionset && rigidbody.velocity.magnitude == 0)
        {
            x = Vector2.Distance(transform.position, mousepos);
        }
        direction = (Vector2)(mousepos - transform.position);
        direction.Normalize();
        rigidbody.AddForce(direction *x* 300);
        hasStriked = true;
    }
    // Update is called once per frame
    void Update()
    {
        line.enabled = false;
        mousepos = Camera.main.ScreenToWorldPoint(Input.mousePosition);
        mouspos2 = new Vector3(-mousepos.x, -mousepos.y-3, mousepos.z);
        if(mouspos2.y> 0.547f)
        {
            mouspos2.y = 0.547f;
        }
        if (mouspos2.y < -2.85f)
        {
            mouspos2.y = -2.85f;
        }
        if (mouspos2.x < -2.45f)
        {
            mouspos2.y = -2.45f;
        }
        if (mouspos2.x > 2.61f)
        {
            mouspos2.x = 2.61f;
        }
        if (!hasStriked && !positionset)
        {
            selftrans.position = new Vector2(myslider.value, startpos.y);
        }
        if (Input.GetMouseButtonUp(0) && rigidbody.velocity.magnitude == 0 && positionset)
        {
            shootstriker();
        }
        RaycastHit2D hit = Physics2D.Raycast(Camera.main.ScreenToWorldPoint(Input.mousePosition), Vector2.zero);
        if (hit.collider != null)
        {
            if (Input.GetMouseButtonDown(0))
            {
                if (!positionset)
                {
                    positionset = true;
                }
            }
        }
        if (positionset && rigidbody.velocity.magnitude == 0)
        {
            line.enabled = true;
            line.SetPosition(0, selftrans.position);
            line.SetPosition(1, mousepos);
        }
        if(rigidbody.velocity.magnitude<0.1f && rigidbody.velocity.magnitude != 0)
        {
            strikerreset();
            bord.GetComponent<gameman>().count++;
        }
    }
    public void strikerreset()
    {
        rigidbody.velocity = Vector2.zero;
        hasStriked = false;
        positionset = false;
        line.enabled = true;
    }
}
